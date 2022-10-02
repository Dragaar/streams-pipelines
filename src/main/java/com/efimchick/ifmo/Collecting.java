package com.efimchick.ifmo;

import com.efimchick.ifmo.util.CourseResult;
import com.efimchick.ifmo.util.Person;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Collecting {

    public int sum(IntStream stream){ return stream.sum();}
    public int production(IntStream stream){ return stream.reduce(1, (a,b) -> a*b);}
    public int oddSum(IntStream stream){
        return stream.filter((x)->(x&1)==1)
                     .sum();}
    public Map<Integer, Integer> sumByRemainder(Integer del, IntStream stream){
        return stream.boxed()
                .collect(Collectors.groupingBy(m->m%del,
                        Collectors.summingInt(s->s)));
    }

    public Map<Person, Double> totalScores(Stream<CourseResult> stream){

        List<CourseResult> resultsList = stream.collect(Collectors.toList());

        Map<Person, Integer> taskResultsSum = resultsList.stream()
                .collect(Collectors.toMap(CourseResult::getPerson,
                                          courseResult -> courseResult.getTaskResults()
                                               .values()
                                               .stream()
                                               .mapToInt(a->a)
                                               .sum()
                                            ));
        long countResults = resultsList.stream()
                .map(courseResult -> courseResult
                        .getTaskResults()
                                .keySet()
                     )
                    .collect(Collectors.toSet())
                    .stream()
                    .flatMap(Collection::stream)
                    .distinct()
                    .count();

        return taskResultsSum.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v-> 1.00 * v.getValue()/countResults));
    }

    public double averageTotalScore(Stream<CourseResult> stream)
    {
        return totalScores(stream).values()
                .stream()
                .collect(Collectors.averagingDouble(m->m));
    }

    public  Map<String, Double> averageScoresPerTask(Stream<CourseResult> stream)
    {
        Set<CourseResult> set = stream.collect(Collectors.toSet());

        long countPersons = set
                .stream()
                .map(CourseResult::getPerson)
                .count();

        Map<String, Double> taskResultsSum = set
                .stream()
                .map(CourseResult::getTaskResults)
                .flatMap(map->map.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.summingDouble(Map.Entry::getValue))
                );

        return taskResultsSum.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v->v.getValue()/countPersons));
    }

    public Map<Person, String> defineMarks(Stream<CourseResult> stream)
    {
        return totalScores(stream).entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v->Mark.getMark(v.getValue())));

    }

    public String easiestTask(Stream<CourseResult> stream){

        return averageScoresPerTask(stream).entrySet()
                .stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .get()
                .getKey();

    }

    public Collector<CourseResult, StringBuilder, String> printableStringCollector()
    {
        final List<CourseResult> list = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();

        return new Collector<>(){
            @Override
            public Supplier<StringBuilder> supplier() {
                return () -> builder;
            }

            @Override
            public BiConsumer<StringBuilder, CourseResult> accumulator() {
                return (s, courseResult) -> list.add(courseResult);
            }

            @Override
            public BinaryOperator<StringBuilder> combiner() {
                return null;
            }

            @Override
            public Function<StringBuilder, String> finisher() {
                return s -> new String(getString(builder, list));
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Set.of(Characteristics.UNORDERED);
            }
        };
    }

    private StringBuilder getString(StringBuilder sb, List<CourseResult> list)
    {
        Map<String, Double> res = averageScoresPerTask(list.stream());
        Map<Person, Double> totalScore = totalScores(list.stream());
        Map<Person, String> defineMark = defineMarks(list.stream());

        List<String> tasks = res.keySet()
                .stream()
                .sorted()
                .collect(Collectors.toList());
        List<Person> persons = list
                .stream()
                .map(CourseResult::getPerson)
                .sorted(Comparator.comparing(Person::getLastName))
                .collect(Collectors.toList());

        int firstColon = list
                .stream()
                .map(e -> (e.getPerson().getLastName() + " " + e.getPerson().getFirstName()+"").length())
                .mapToInt(e->e+1)
                .max().orElse(10);

        sb.append(String.format("%-" +firstColon + "s|", "Student "));
        sb.append(tasks
                .stream()
                .map(e->" "+e).collect(Collectors.joining(" |"))
        );
        sb.append(" | Total | Mark |\n");

        sb.append(persons.stream()
                .map(p->{
                    sb.append(String.format("%-"+firstColon+"s|", p.getLastName() +" " + p.getFirstName()+" "));

                    CourseResult courseResult = list.stream()
                            .filter(e->e.getPerson().equals(p))
                            .findFirst().get();

                    sb.append(tasks.stream()
                            .map(e->String.format("%"+(e.length()+2) + "s|",
                                    courseResult.getTaskResults().get(e)==null?0+" ":courseResult.getTaskResults().get(e)+ " "))
                                            .collect(Collectors.joining()));

                    sb.append(String.format("%" + ("Total".length()+2) + "s|", String.format(Locale.US, "%.2f", totalScore.get(p)==null?0 :Math.round(totalScore.get(p)*100)/100.00) + " "));
                    sb.append(String.format("%" + ("Mark".length()+2) + "s|", defineMark.get(p) + " "));
                    sb.append("\n");
                    return "";

                }).collect(Collectors.joining()));
        sb.append(String.format("%-"+firstColon+"s|", "Average "));

        double total = res.values().stream().collect(Collectors.averagingDouble(e->e));

        sb.append(tasks
                .stream()
                .map(e-> String.format("%" + (e.length()+2) + "s|",
                        String.format(Locale.US, "%.2f",
                                Math.round(res.get(e)*100)/100.00) + " "))
                .collect(Collectors.joining()));
        sb.append(String.format("%" + ("Total".length()+2) + "s|", String.format(Locale.US, "%.2f", Math.round(total*100)/100.00) + " "));
        sb.append(String.format("%" + ("Mark".length()+2) + "s|", Mark.getMark(total) + " "));

        return sb;
    }
    enum Mark{
        A(90, 100), B(83, 90), C(75, 83), D(68, 75), E(60, 68), F(0, 60);

        Mark(final int min, final int max){
            this.min = min;
            this.max = max;
        }

        private final int max;
        private final int min;

        public static String getMark(double result){
            return Arrays.stream(Mark.values())
                    .filter(s-> s.min <= result && result < s.max)
                    .findFirst()
                    .orElse(F)
                    .toString();
        }
    }
}
