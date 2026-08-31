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

    @Test void seedsExternalHeadlineLibraryAndAllowsUserChanges() throws Exception {
        TestDataSupport data=new TestDataSupport(temp);
        DashboardHeadlineService service=new DashboardHeadlineService(new DashboardHeadlineRepository(data.json,data.mapper));
        assertThat(service.all()).hasSizeGreaterThanOrEqualTo(20);
        assertThat(service.random()).isIn(service.all().stream().map(v->v.text()).toList());
        var first=service.add("用户写下的第一句。");
        var second=service.add("用户写下的第二句。");
        assertThat(service.all()).extracting(v->v.text()).contains(first.text(),second.text());
        service.remove(first.id());
        assertThat(service.all()).extracting(v->v.text()).doesNotContain(first.text()).contains(second.text());
    }

    @Test void rejectsBlankDuplicateAndOverlongCopy() throws Exception {
        TestDataSupport data=new TestDataSupport(temp);
        DashboardHeadlineService service=new DashboardHeadlineService(new DashboardHeadlineRepository(data.json,data.mapper));
        service.add("不重复");
        assertThatThrownBy(()->service.add("  ")).hasMessageContaining("文案不能为空");
        assertThatThrownBy(()->service.add("不重复")).hasMessageContaining("已经存在");
        assertThatThrownBy(()->service.add("长".repeat(81))).hasMessageContaining("80");
        assertThatThrownBy(()->service.remove("default-01")).hasMessageContaining("内置文案不能删除");
    }

    @Test void mergesBuiltInsWithAnExistingLegacyCustomFile() throws Exception {
        TestDataSupport data=new TestDataSupport(temp);
        data.json.write("dashboard-headlines.json", java.util.List.of(
                new io.github.aomckin.lifehud.domain.DashboardHeadline("old-default","真挚！与热诚！",java.time.Instant.now()),
                new io.github.aomckin.lifehud.domain.DashboardHeadline("custom-1","只属于我的一句。",java.time.Instant.now())));
        DashboardHeadlineService service=new DashboardHeadlineService(new DashboardHeadlineRepository(data.json,data.mapper));
        assertThat(service.all()).hasSize(21);
        assertThat(service.all()).extracting(v->v.text()).contains("真挚！与热诚！","今天从这里开始。","只属于我的一句。");
    }
}
