import { api } from "../api/client.js?v=0.5.1";
import { statusLabels, milestoneStatusLabels, directionCopy as c } from "../content/copy.js?v=0.5.1";
import { empty, error, escapeHtml, toast } from "../components/ui.js";

const date = value => value ? new Intl.DateTimeFormat("zh-CN", {year:"numeric",month:"short",day:"numeric"}).format(new Date(value)) : "";
let selectedId = location.hash.slice(1) || "";
let filter = "ACTIVE";

export async function dreams(root) {
  root.innerHTML = '<section class="panel">正在整理梦想…</section>';
  try {
    const dreams = await api.dreams.all();
    if (selectedId && dreams.some(d => d.id === selectedId)) return renderDetail(root, selectedId);
    selectedId = "";
    renderList(root, dreams);
  } catch (reason) { root.innerHTML = error(reason.message); }
}

function renderList(root, dreams) {
  const groups = ["ACTIVE", "PAUSED", "COMPLETED", "ARCHIVED"];
  const visible = dreams.filter(d => d.status === filter);
  root.innerHTML = `<div class="direction-page"><section class="panel"><div class="section-head"><div><div class="eyebrow">v0.5 · Direction</div><h2>我正在追逐什么</h2></div><button class="button button-primary" id="new-dream">${c.newDream}</button></div>
  <nav class="growth-tabs status-tabs">${groups.map(s=>`<button class="${filter===s?"active":""}" data-filter="${s}">${statusLabels[s]} ${dreams.filter(d=>d.status===s).length}</button>`).join("")}</nav>
  <div class="dream-grid">${visible.map(d=>`
    <article class="card dream-card" data-open="${d.id}">
      ${d.coverImagePath?`<img class="dream-cover" src="${escapeHtml(d.coverImagePath)}" alt="">`:""}
      <div class="dream-card-body"><span class="badge">${statusLabels[d.status]}</span>
      <h3>${escapeHtml(d.title)}</h3>
      ${d.meaning?`<p class="dream-meaning">${escapeHtml(d.meaning)}</p>`:""}
      <small>${d.targetDate?`目标 ${date(d.targetDate)}`:"没有设定期限"}</small></div>
    </article>`).join("")||empty(filter==="ACTIVE"?c.emptyActive:c.emptyFiltered)}</div></section>
  <form class="milestone-form" id="dream-form" hidden>
    <label class="field"><span>${c.titleLabel}</span><input id="dream-title" maxlength="120" required></label>
    <label class="field"><span>${c.meaningLabel}</span><input id="dream-meaning" maxlength="200" placeholder="${c.meaningPlaceholder}"></label>
    <label class="field"><span>${c.descriptionLabel}</span><textarea id="dream-description" rows="3"></textarea></label>
    <div class="milestone-form-grid">
      <label class="field"><span>${c.targetDateLabel}</span><input id="dream-target" type="date"></label>
      <label class="field"><span>${c.coverLabel}</span><input id="dream-cover" type="file" accept="image/*"></label>
    </div>
    <label class="field"><span>${c.noteLabel}</span><input id="dream-note" maxlength="300"></label>
    <div><button class="button button-primary" type="submit">${c.save}</button><button class="button button-ghost" type="button" id="cancel-dream">${c.cancel}</button></div>
  </form></div>`;
  root.querySelectorAll("[data-filter]").forEach(b=>b.addEventListener("click",()=>{filter=b.dataset.filter;dreams(root);}));
  root.querySelectorAll("[data-open]").forEach(card=>card.addEventListener("click",()=>{selectedId=card.dataset.open;history.replaceState({},"",`/dreams#${selectedId}`);renderDetail(root,selectedId);}));
  const form=root.querySelector("#dream-form");
  root.querySelector("#new-dream").addEventListener("click",()=>{form.reset();form.hidden=false;root.querySelector("#dream-title").focus();});
  root.querySelector("#cancel-dream").addEventListener("click",()=>form.hidden=true);
  form.addEventListener("submit",async event=>{
    event.preventDefault();
    try{
      const cover=await uploadCover(form);
      await api.dreams.create({title:v("dream-title"),meaning:v("dream-meaning"),description:v("dream-description"),
        targetDate:v("dream-target")||null,coverImagePath:cover,note:v("dream-note"),status:"ACTIVE"});
      toast("梦想已经立下了");dreams(root);
    }catch(reason){toast(reason.message,true);}
  });

  function v(id){return root.querySelector("#"+id).value.trim();}
  async function uploadCover(form){
    const file=form.querySelector("#dream-cover").files[0];
    if(!file)return "";
    const data=new FormData();data.append("file",file);
    const result=await api.uploadImage(data);
    return result.path;
  }
}

async function renderDetail(root, id) {
  root.innerHTML = '<section class="panel">正在展开这个梦想…</section>';
  try {
    const detail = await api.dreams.detail(id);
    const linked = await api.taskDirections.all();
    const dream = detail.dream;
    const linkedTasks = [...linked.daily, ...linked.special].filter(t =>
      [t.direction.dreamId, t.direction.goalId, t.direction.dreamMilestoneId].includes(id));
    const milestones = detail.goals.flatMap(g => g.milestones);
    const doneMilestones = milestones.filter(m => m.status === "COMPLETED").length;
    root.innerHTML = `<div class="direction-page"><section class="panel dream-detail">
      <button class="text-link" id="back-dreams">${c.backToList}</button>
      <div class="dream-detail-head">
        ${dream.coverImagePath?`<img class="dream-cover big" src="${escapeHtml(dream.coverImagePath)}" alt="">`:""}
        <div><span class="badge">${statusLabels[dream.status]}</span><h2>${escapeHtml(dream.title)}</h2>
        ${dream.meaning?`<p class="dream-meaning">${escapeHtml(dream.meaning)}</p>`:""}
        ${dream.description?`<p class="dream-desc">${escapeHtml(dream.description)}</p>`:""}
        <small>${dream.targetDate?`目标 ${date(dream.targetDate)} · `:""}${milestones.length?`${doneMilestones} / ${milestones.length} 里程碑`:"还没有拆里程碑"}</small></div>
      </div>
      <div class="row-actions dream-actions">
        ${dream.status!=="COMPLETED"?`<button class="button button-primary" data-act="complete">${c.complete}</button>`:""}
        ${dream.status==="ACTIVE"?`<button class="button button-ghost" data-act="pause">${c.pause}</button>`:""}
        ${dream.status==="PAUSED"?`<button class="button button-ghost" data-act="resume">${c.resume}</button>`:""}
        ${dream.status!=="ARCHIVED"?`<button class="button button-ghost" data-act="archive">${c.archive}</button>`:""}
        <button class="text-link" data-act="edit">${c.edit}</button>
      </div>
      <form class="milestone-form" id="dream-edit-form" hidden></form>
    </section>
    <section class="panel"><div class="section-head"><h2>${c.goalsTitle}</h2><button class="text-link" id="new-goal">${c.newGoal}</button></div>
      <form class="milestone-form goal-form" id="goal-form" hidden><label class="field"><span>${c.goalTitle}</span><input id="goal-title" maxlength="120" required></label>
      <div><button class="button button-primary" type="submit">${c.save}</button><button class="button button-ghost" type="button" id="cancel-goal">${c.cancel}</button></div></form>
      ${detail.goals.map(({goal, milestones})=>`
        <div class="goal-block ${goal.status==="COMPLETED"?"done":""}">
          <div class="goal-head"><strong>${escapeHtml(goal.title)}</strong><span class="badge">${statusLabels[goal.status]}</span>
          <div class="row-actions">
            ${goal.status!=="COMPLETED"?`<button class="text-link" data-complete-goal="${goal.id}">${c.goalDone}</button>`:""}
            <button class="text-link danger-link" data-delete-goal="${goal.id}">${c.delete}</button></div></div>
          <div class="milestone-list">
            ${milestones.map(m=>`<div class="milestone-row ${m.status==="COMPLETED"?"done":""}">
              <button class="milestone-check ${m.status==="COMPLETED"?"checked":""}" data-toggle-milestone="${m.id}">${m.status==="COMPLETED"?"✓":"○"}</button>
              <span>${escapeHtml(m.title)}</span>
              <button class="text-link danger-link" data-delete-milestone="${m.id}">${c.delete}</button></div>`).join("")}
            <form class="inline-form" data-goal="${goal.id}"><input placeholder="${c.milestoneTitle}" maxlength="120" required><button class="button button-secondary" type="submit">${c.newMilestone}</button></form>
          </div>
        </div>`).join("")||empty("还没有方向。把梦想拆成几件可执行的事。")}
    </section>
    <section class="panel"><div class="section-head"><h2>${c.linkedTasks}</h2></div>
      ${linkedTasks.map(t=>`<div class="milestone-row"><span class="badge">${t.source==="daily"?"每日":"特殊"}</span><strong>${escapeHtml(t.name)}</strong>
        <small>${directionText(t.direction)}</small></div>`).join("")||empty(c.noLinkedTasks)}
    </section></div>`;
  } catch (reason) { root.innerHTML = error(reason.message); }

  bind();

  function directionText(d){
    return [d.dreamTitle,d.goalTitle,d.dreamMilestoneTitle].filter(Boolean).join(" › ");
  }
  function v(id){return root.querySelector("#"+id)?.value.trim()||"";}
  function bind(){
    root.querySelector("#back-dreams").addEventListener("click",()=>{selectedId="";history.replaceState({},"","/dreams");dreams(root);});
    root.querySelectorAll("[data-act]").forEach(button=>button.addEventListener("click",async()=>{
      const act=button.dataset.act;
      try{
        if(act==="complete")await api.dreams.complete(id);
        else if(act==="pause")await api.dreams.pause(id);
        else if(act==="resume")await api.dreams.resume(id);
        else if(act==="archive"){if(!confirm(c.archiveConfirm))return;await api.dreams.archive(id);}
        else if(act==="edit")return openEdit();
        toast("已更新");renderDetail(root,id);
      }catch(reason){toast(reason.message,true);}
    }));
    root.querySelector("#new-goal").addEventListener("click",()=>{root.querySelector("#goal-form").hidden=false;root.querySelector("#goal-title").focus();});
    root.querySelector("#cancel-goal").addEventListener("click",()=>root.querySelector("#goal-form").hidden=true);
    root.querySelector("#goal-form").addEventListener("submit",async event=>{
      event.preventDefault();
      try{await api.dreams.createGoal(id,{title:v("goal-title")});toast("新方向已立下");renderDetail(root,id);}
      catch(reason){toast(reason.message,true);}
    });
    root.querySelectorAll("form.inline-form").forEach(form=>form.addEventListener("submit",async event=>{
      event.preventDefault();
      const input=form.querySelector("input");
      if(!input.value.trim())return;
      try{await api.dreams.createMilestone(form.dataset.goal,{title:input.value.trim()});renderDetail(root,id);}
      catch(reason){toast(reason.message,true);}
    }));
    root.querySelectorAll("[data-toggle-milestone]").forEach(button=>button.addEventListener("click",async()=>{
      try{
        const detail=await api.dreams.detail(id);
        const m=detail.goals.flatMap(g=>g.milestones).find(x=>x.id===button.dataset.toggleMilestone);
        if(m.status==="COMPLETED")await api.dreams.updateMilestone(m.id,{title:m.title,status:"PENDING"});
        else await api.dreams.completeMilestone(m.id);
        renderDetail(root,id);
      }catch(reason){toast(reason.message,true);}
    }));
    root.querySelectorAll("[data-delete-milestone]").forEach(button=>button.addEventListener("click",async()=>{
      if(!confirm(c.deleteConfirm))return;
      try{await api.dreams.deleteMilestone(button.dataset.deleteMilestone);renderDetail(root,id);}
      catch(reason){toast(reason.message,true);}
    }));
    root.querySelectorAll("[data-complete-goal]").forEach(button=>button.addEventListener("click",async()=>{
      try{await api.dreams.completeGoal(button.dataset.completeGoal);toast("方向已完成");renderDetail(root,id);}
      catch(reason){toast(reason.message,true);}
    }));
    root.querySelectorAll("[data-delete-goal]").forEach(button=>button.addEventListener("click",async()=>{
      if(!confirm(c.deleteConfirm))return;
      try{await api.dreams.deleteGoal(button.dataset.deleteGoal);renderDetail(root,id);}
      catch(reason){toast(reason.message,true);}
    }));
  }
  async function openEdit(){
    const form=root.querySelector("#dream-edit-form");
    const d=(await api.dreams.detail(id)).dream;
    form.innerHTML=`<label class="field"><span>${c.titleLabel}</span><input id="edit-title" maxlength="120" value="${escapeHtml(d.title)}" required></label>
    <label class="field"><span>${c.meaningLabel}</span><input id="edit-meaning" maxlength="200" value="${escapeHtml(d.meaning||"")}"></label>
    <label class="field"><span>${c.descriptionLabel}</span><textarea id="edit-description" rows="3">${escapeHtml(d.description||"")}</textarea></label>
    <div class="milestone-form-grid">
      <label class="field"><span>${c.targetDateLabel}</span><input id="edit-target" type="date" value="${d.targetDate||""}"></label>
      <label class="field"><span>${c.coverLabel}</span><input id="edit-cover" type="file" accept="image/*"></label>
    </div>
    <label class="field"><span>${c.noteLabel}</span><input id="edit-note" maxlength="300" value="${escapeHtml(d.note||"")}"></label>
    <div><button class="button button-primary" type="submit">${c.save}</button><button class="button button-ghost" type="button" id="cancel-edit">${c.cancel}</button></div>`;
    form.hidden=false;
    form.onsubmit=async event=>{
      event.preventDefault();
      try{
        let cover=d.coverImagePath;
        const file=form.querySelector("#edit-cover").files[0];
        if(file){const data=new FormData();data.append("file",file);cover=(await api.uploadImage(data)).path;}
        await api.dreams.update(id,{title:form.querySelector("#edit-title").value.trim(),
          meaning:form.querySelector("#edit-meaning").value.trim(),description:form.querySelector("#edit-description").value,
          targetDate:form.querySelector("#edit-target").value||null,coverImagePath:cover,
          note:form.querySelector("#edit-note").value.trim(),status:d.status});
        toast("梦想已更新");renderDetail(root,id);
      }catch(reason){toast(reason.message,true);}
    };
    root.querySelector("#cancel-edit").addEventListener("click",()=>form.hidden=true);
  }
}
