package io.battery.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit-test")
public class RandomDataTest {
    enum SampleEnum {
        ONE, TWO, THREE
    }

    @Test
    public void testSelectRandomFromEnum() {
        SampleEnum sample = RandomData.selectRandom(SampleEnum.class);
        assertThat(sample).isNotNull();
    }

    @Test
    public void testSelectRandomFromList() {
        List<String> list = List.of("A", "B", "C");
        String item = RandomData.selectRandom(list);
        assertThat(list).contains(item);
    }

    @Test
    public void testSelectRandomFromSet() {
        Set<String> set = Set.of("X", "Y", "Z");
        String item = RandomData.selectRandom(set);
        assertThat(set).contains(item);
    }

    @Test
    public void testSelectRandomFromArray() {
        String[] array = new String[] {"1", "2", "3"};
        String item = RandomData.selectRandom(array);
        assertThat(array).contains(item);
    }

    @Test
    public void testSelectRandomUnique() {
        List<Integer> list = List.of(1, 2, 3, 4, 5);
        Collection<Integer> unique = RandomData.selectRandomUnique(list, 3);
        assertThat(unique).hasSize(3);

        assertThatThrownBy(() -> RandomData.selectRandomUnique(list, 10))
                .isInstanceOf(IllegalArgumentException.class);

        Integer[] array = new Integer[] {1, 2, 3, 4, 5};
        Collection<Integer> uniqueArray = RandomData.selectRandomUnique(array, 2);
        assertThat(uniqueArray).hasSize(2);

        assertThatThrownBy(() -> RandomData.selectRandomUnique(array, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testSelectRandomUniqueWithDuplicates() {
        // Previously looped forever, since duplicates can't make up the count
        List<Integer> list = List.of(1, 1, 2, 2);
        assertThat(RandomData.selectRandomUnique(list, 2)).containsExactlyInAnyOrder(1, 2);

        assertThatThrownBy(() -> RandomData.selectRandomUnique(list, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Not enough unique elements: 2 of 3");
    }

    @Test
    public void testSelectRandomWeighted() {
        List<String> rawItems = List.of("A", "B");
        List<Double> weights = List.of(5.0, 10.0);
        String chosenRaw = RandomData.selectRandomWeighted(rawItems, weights);
        assertThat(chosenRaw).isNotNull();

        assertThatThrownBy(() ->
                RandomData.selectRandomWeighted(Collections.emptyList(), Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                RandomData.selectRandomWeighted(rawItems, List.of(1.0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testWordBreaks() {
        String text = "The quick brown fox jumps over the lazy dog";
        String broken = RandomData.wordBreaks(text, 10);
        assertThat(broken).isNotNull().contains("\n");
    }
}
