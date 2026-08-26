package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.dto.OperationResult;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public final class TitleService {
    private final TitleSystem titles;
    private final GameQueryService queries;

    public TitleService(TitleSystem titles, GameQueryService queries) {
        this.titles = titles; this.queries = queries;
    }

    public OperationResult equip(String id) {
        if (!titles.equip(id)) return queries.error("称号不可佩戴");
        return new OperationResult(true, "称号已佩戴", List.of(), queries.state());
    }
}
