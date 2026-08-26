package io.github.aomckin.lifehud.service;

import java.util.*;

public abstract class UnlockableSystem {
    protected final List<Map<String,Object>> items;
    protected UnlockableSystem(List<Map<String,Object>> items){this.items=List.copyOf(items);}
    public List<Map<String,Object>> check(Object context){List<Map<String,Object>> out=new ArrayList<>();for(var item:items){String id=Objects.toString(item.get("id"),"");if(!id.isBlank()&&!isUnlocked(id)&&isDone(item,context))out.add(item);}return out;}
    public boolean unlock(Map<String,Object> item){String id=Objects.toString(item.get("id"),"");return !id.isBlank()&&!isUnlocked(id);}
    public boolean isUnlocked(Object itemOrId){String id=itemOrId instanceof Map<?,?> m?Objects.toString(m.get("id"),""):Objects.toString(itemOrId,"");return unlockedIds().contains(id);}
    protected abstract Collection<String> unlockedIds(); protected abstract boolean isDone(Map<String,Object> item,Object context);
    public List<Map<String,Object>> items(){return items;}
}
