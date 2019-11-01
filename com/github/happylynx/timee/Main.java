package com.github.happylynx.timee;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import sun.misc.Signal;

public class Main {

    private static Instant start;
    private static boolean done = false;

    public static void main(String[] args) {
        registerHook();
        start = Instant.now();
        runProcess(args);
        finish();
    }

    private static synchronized void finish() {
        final Instant end = Instant.now();
        if (done) {
            return;
        }
        done = true;
        final Duration executionTime = Duration.between(start, end);
        System.out.println();
        System.out.println("Started  " + toString(start));
        System.out.println("Finished " + toString(end));
        System.out.println("Duration " + toString(executionTime));
        System.out.flush();
    }

    private static void registerHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdownHook));
    }

    private static void shutdownHook() {
        finish();
    }

    private static String toString(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd kk:mm:ss z").withZone(ZoneId.systemDefault()).format(instant);
    }

    private static String toString(Duration duration) {
        final long secondsInDay = 24 * 60 * 60;
        final long secondsInHour = 60 * 60;
        final long secondsInMinute = 60;
        final List<Long> splitted =
                split(duration.getSeconds(), Arrays.asList(secondsInDay, secondsInHour, secondsInMinute, 1L));
        final List<String> unitNames = Arrays.asList("d", "h", "m", "s");
        final String secondsPart = IntStream.range(0, splitted.size())
                .mapToObj(index -> {
                    final Long value = splitted.get(index);
                    return value == 0 ? "" : (value.toString() + unitNames.get(index));
                })
                .filter(unitStringified -> !unitStringified.isEmpty())
                .collect(Collectors.joining(" "));
        final String nanosPart = String.format("%,dms", duration.getNano() / 1_000_000);
        return secondsPart + (secondsPart.isEmpty() ? "" : " ") + nanosPart;
    }

    private static List<Long> split(long value, List<Long> unitSizes) {
        long remainingValue = value;
        final List<Long> result = new ArrayList<>();
        for (Long unitSize : unitSizes) {
            result.add(remainingValue / unitSize);
            remainingValue %= unitSize;
        }
        return result;
    }

    private static void runProcess(String[] args) {
        if (args.length == 0) {
            return;
        }
        Signal.handle(new Signal("INT"), sig -> System.exit(0));
        try {
            final Process process = new ProcessBuilder(args).inheritIO().start();
            process.waitFor();
        } catch (IOException | InterruptedException ex) {
            System.err.println(ex.getMessage());
        }
    }
}