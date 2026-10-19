package ru.mirea.kinostudio.util;

import ru.mirea.kinostudio.model.RequestView;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Экспорт заявок в CSV (разделитель «;», UTF-8 с BOM — чтобы Excel правильно показал кириллицу).
 */
public final class CsvExporter {

    private static final String SEP = ";";

    private CsvExporter() {
    }

    public static void exportRequests(List<RequestView> views, Path file) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write('﻿'); // BOM
            w.write(String.join(SEP, "ID", "Название", "Клиент", "Локация", "Дата съёмки",
                    "Длительность (ч)", "Бюджет", "Статус"));
            w.newLine();
            for (RequestView v : views) {
                w.write(String.join(SEP,
                        String.valueOf(v.getId()),
                        escape(v.getTitle()),
                        escape(v.getClientName()),
                        escape(v.getLocationName()),
                        Labels.date(v.getShootingDate()),
                        String.valueOf(v.getDurationHours()),
                        v.getBudget().toPlainString().replace('.', ','),
                        escape(Labels.status(v.getStatus()))));
                w.newLine();
            }
        }
    }

    /** Поле с «;», кавычками или переводом строки берётся в кавычки, внутренние кавычки удваиваются. */
    static String escape(String value) {
        if (value == null) return "";
        String v = value.replace("\r", " ").replace("\n", " ");
        if (v.contains(SEP) || v.contains("\"")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
