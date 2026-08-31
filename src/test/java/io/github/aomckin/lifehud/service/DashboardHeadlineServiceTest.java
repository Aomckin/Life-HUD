package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.TestDataSupport;
import io.github.aomckin.lifehud.repository.DashboardHeadlineRepository;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardHeadlineServiceTest {
    @TempDir Path temp;

    @Test void usesDefaultUntilCustomHeadlinesAreAdded() throws Exception {
        TestDataSupport data=new TestDataSupport(temp);
        DashboardHeadlineService service=new DashboardHeadlineService(new DashboardHeadlineRepository(data.json,data.mapper));
        assertThat(service.random()).isEqualTo(DashboardHeadlineService.DEFAULT);
        var first=service.add("今天也向前一点。");
        var second=service.add("把注意力交给真正重要的事。");
        assertThat(service.all()).extracting(v->v.text()).containsExactly(first.text(),second.text());
        assertThat(service.random()).isIn(first.text(),second.text());
        service.remove(first.id());
        assertThat(service.all()).extracting(v->v.text()).containsExactly(second.text());
    }

    @Test void rejectsBlankDuplicateAndOverlongCopy() throws Exception {
        TestDataSupport data=new TestDataSupport(temp);
        DashboardHeadlineService service=new DashboardHeadlineService(new DashboardHeadlineRepository(data.json,data.mapper));
        service.add("不重复");
        assertThatThrownBy(()->service.add("  ")).hasMessageContaining("文案不能为空");
        assertThatThrownBy(()->service.add("不重复")).hasMessageContaining("已经存在");
        assertThatThrownBy(()->service.add("长".repeat(81))).hasMessageContaining("80");
    }
}
