package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.domain.DashboardHeadline;
import io.github.aomckin.lifehud.repository.DashboardHeadlineRepository;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public final class DashboardHeadlineService {
    public static final String DEFAULT="把今天过成自己喜欢的样子。";
    private final DashboardHeadlineRepository repository;
    public DashboardHeadlineService(DashboardHeadlineRepository repository){this.repository=repository;}
    public List<DashboardHeadline> all(){return repository.all();}
    public String random(){List<DashboardHeadline> values=all();return values.isEmpty()?DEFAULT:values.get(ThreadLocalRandom.current().nextInt(values.size())).text();}
    public DashboardHeadline add(String text){String value=text==null?"":text.trim();if(value.isBlank())throw bad("文案不能为空");if(value.length()>80)throw bad("文案不能超过 80 个字符");if(all().stream().anyMatch(v->v.text().equals(value)))throw bad("这句文案已经存在");return repository.save(new DashboardHeadline(UUID.randomUUID().toString(),value,Instant.now()));}
    public void remove(String id){if(!repository.remove(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"文案不存在");}
    private ResponseStatusException bad(String text){return new ResponseStatusException(HttpStatus.BAD_REQUEST,text);}
}
