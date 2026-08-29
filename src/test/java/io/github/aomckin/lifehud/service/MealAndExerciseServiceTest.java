package io.github.aomckin.lifehud.service;

import io.github.aomckin.lifehud.TestDataSupport;
import io.github.aomckin.lifehud.V05TestWiring;
import io.github.aomckin.lifehud.domain.ExerciseRequest;
import io.github.aomckin.lifehud.domain.LifeEventType;
import io.github.aomckin.lifehud.domain.MealRecord;
import io.github.aomckin.lifehud.domain.MealRequest;
import io.github.aomckin.lifehud.domain.MealType;
import io.github.aomckin.lifehud.repository.ExerciseRecordRepository;
import io.github.aomckin.lifehud.repository.MealRecordRepository;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** v0.6 meal + exercise facts: images survive edits, facts follow the record. */
class MealAndExerciseServiceTest {
    @TempDir Path temp; TestDataSupport data; V05TestWiring w;
    MealService meals; ExerciseService exercises;

    @BeforeEach void setup() throws Exception {
        data = new TestDataSupport(temp); data.baseline(100, 0, 0); w = V05TestWiring.create(data);
        meals = new MealService(new MealRecordRepository(data.json, data.mapper),
                new LifeFactRecorder(w.events), new GrowthCopy(data.json));
        exercises = new ExerciseService(new ExerciseRecordRepository(data.json, data.mapper),
                new LifeFactRecorder(w.events), new GrowthCopy(data.json));
    }

    private MealRecord meal() {
        return meals.create(new MealRequest("DINNER", Instant.parse("2026-08-29T10:42:00Z"),
                "辣椒炒肉 + 米饭", 4, "", List.of("/uploads/one.jpg", "/uploads/two.jpg")));
    }

    @Test void mealKeepsDescriptionImagesAndSatisfaction(){
        MealRecord value = meal();
        assertThat(value.mealType()).isEqualTo(MealType.DINNER);
        assertThat(value.images()).containsExactly("/uploads/one.jpg", "/uploads/two.jpg");
        var event = w.lifeEvents.all().stream().filter(v -> v.type() == LifeEventType.MEAL_RECORDED).findFirst().orElseThrow();
        assertThat(event.title()).isEqualTo("晚餐");
        assertThat(event.content()).contains("辣椒炒肉").contains("4/5");
        assertThat(event.metadata()).containsEntry("images", List.of("/uploads/one.jpg", "/uploads/two.jpg"));
    }

    @Test void updatingWithoutImageFieldKeepsExistingImages(){
        MealRecord value = meal();
        meals.update(value.id(), new MealRequest(null, null, "辣椒炒肉 + 杂粮饭", 5, null, null));
        assertThat(meals.get(value.id()).images()).hasSize(2);
        assertThat(meals.get(value.id()).description()).contains("杂粮饭");
        var events = w.lifeEvents.all().stream().filter(v -> v.type() == LifeEventType.MEAL_RECORDED).toList();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).content()).contains("5/5");
    }

    @Test void removingImagesOnUpdateSyncsTheFact(){
        MealRecord value = meal();
        meals.update(value.id(), new MealRequest(null, null, null, null, null, List.of("/uploads/one.jpg")));
        assertThat(meals.get(value.id()).images()).containsExactly("/uploads/one.jpg");
        var event = w.lifeEvents.all().stream().filter(v -> v.type() == LifeEventType.MEAL_RECORDED).findFirst().orElseThrow();
        assertThat(event.metadata()).containsEntry("images", List.of("/uploads/one.jpg"));
    }

    @Test void deletingAMealDeletesItsFact(){
        MealRecord value = meal();
        meals.remove(value.id());
        assertThat(w.lifeEvents.all()).noneMatch(v -> v.type() == LifeEventType.MEAL_RECORDED);
    }

    @Test void exerciseFactsCarryTypeDurationAndIntensity(){
        exercises.create(new ExerciseRequest("MAIMAI", Instant.parse("2026-08-29T12:16:00Z"), 86, "MEDIUM", ""));
        var event = w.lifeEvents.all().stream().filter(v -> v.type() == LifeEventType.EXERCISE_RECORDED).findFirst().orElseThrow();
        assertThat(event.title()).isEqualTo("舞萌");
        assertThat(event.content()).isEqualTo("86 min · 中等强度");
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-08-29T12:16:00Z"));
    }

    @Test void illegalExerciseDurationsAreRejected(){
        assertThatThrownBy(() -> exercises.create(new ExerciseRequest("WALK", null, 0, null, null)))
                .hasMessageContaining("1 ~ 1440");
        assertThatThrownBy(() -> exercises.create(new ExerciseRequest("WALK", null, null, null, null)))
                .hasMessageContaining("1 ~ 1440");
    }
}
