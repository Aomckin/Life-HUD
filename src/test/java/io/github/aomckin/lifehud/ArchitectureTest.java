package io.github.aomckin.lifehud;

import io.github.aomckin.lifehud.core.GameCommandFacade;import io.github.aomckin.lifehud.domain.Player;import io.github.aomckin.lifehud.service.PlayerService;import org.junit.jupiter.api.Test;import java.lang.reflect.Method;import java.nio.file.*;import static org.assertj.core.api.Assertions.assertThat;
class ArchitectureTest {
 @Test void allJavaCodeUsesRequiredRootPackage()throws Exception{try(var files=Files.walk(Path.of("src"))){for(Path p:files.filter(x->x.toString().endsWith(".java")).toList()){String text=Files.readString(p);assertThat(text).as(p.toString()).contains("package io.github.aomckin.lifehud");}}}
 @Test void playerEntityHasNoBusinessMethods(){assertThat(Player.class.getDeclaredMethods()).extracting(Method::getName).allMatch(n->n.startsWith("lambda$")||n.equals("equals")||n.equals("hashCode")||n.equals("toString"));}
 @Test void playerServiceHasNoRepositoryDependency(){assertThat(PlayerService.class.getDeclaredFields()).isEmpty();}
 @Test void webUiUsesShellAndDedicatedGrowthPage()throws Exception{String shell=Files.readString(Path.of("src/main/resources/static/v0.2.html"));String routes=Files.readString(Path.of("src/main/resources/static/modules/navigation.js"));String growth=Files.readString(Path.of("src/main/resources/static/pages/growth.js"));assertThat(shell).contains("Life HUD v0.4","growth.css");assertThat(routes).contains("/dashboard","/focus","/growth");assertThat(growth).contains("Achievements","Milestones","Titles").doesNotContain("Coin","Shop");}
 @Test void businessServicesDoNotDependOnGlobalGameCore()throws Exception{try(var files=Files.walk(Path.of("src/main/java/io/github/aomckin/lifehud/service"))){for(Path p:files.filter(x->x.toString().endsWith(".java")).toList()){String text=Files.readString(p);assertThat(text).as(p.toString()).doesNotContain("GameCore");}}
 }
 @Test void commandFacadeOwnsCommandRouting(){assertThat(GameCommandFacade.class.isAnnotationPresent(org.springframework.stereotype.Component.class)).isTrue();}
 @Test void genericJsonStoreHasNoDomainSpecificKnowledge()throws Exception{String text=Files.readString(Path.of("src/main/java/io/github/aomckin/lifehud/repository/JsonFileStore.java"));assertThat(text).doesNotContain("Player").doesNotContain("Achievement").doesNotContain("Task").doesNotContain("Log");}
}
