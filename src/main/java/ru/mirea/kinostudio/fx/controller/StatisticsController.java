package ru.mirea.kinostudio.fx.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.mirea.kinostudio.fx.AppContext;
import ru.mirea.kinostudio.fx.Dialogs;
import ru.mirea.kinostudio.model.RequestStatus;
import ru.mirea.kinostudio.model.RequestView;
import ru.mirea.kinostudio.service.StatisticsService.LocationStat;
import ru.mirea.kinostudio.service.StatisticsService.Statistics;
import ru.mirea.kinostudio.util.Labels;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Вкладка «Статистика»: карточки с показателями, диаграмма по статусам и таблица по локациям. */
public class StatisticsController {

    private final AppContext ctx = AppContext.get();

    @FXML private Label totalValue;
    @FXML private Label sumValue;
    @FXML private Label avgValue;
    @FXML private Label minMaxValue;
    @FXML private Label durationValue;
    @FXML private Label upcomingValue;
    @FXML private Label completedValue;
    @FXML private Label topLocationValue;
    @FXML private Label upcomingCaption;
    @FXML private BarChart<String, Number> statusChart;
    @FXML private TableView<LocationStat> locationTable;
    @FXML private TableColumn<LocationStat, String> locNameCol;
    @FXML private TableColumn<LocationStat, Integer> locCountCol;
    @FXML private TableColumn<LocationStat, String> locSumCol;

    @FXML
    private void initialize() {
        locNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().locationName()));
        locCountCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().requests()));
        locSumCol.setCellValueFactory(c -> new SimpleStringProperty(Labels.money(c.getValue().totalBudget())));
        locCountCol.getStyleClass().add("right");
        locSumCol.getStyleClass().add("right");
        locationTable.getColumns().forEach(c -> c.setSortable(false));
        upcomingCaption.setText("Ближайшие " + ru.mirea.kinostudio.service.StatisticsService.UPCOMING_DAYS
                + " дн. (не закрытые)");
        ctx.addListener(this::refresh);
        refresh();
    }

    private void refresh() {
        List<RequestView> views;
        try {
            views = ctx.viewService.loadAll();
        } catch (SQLException e) {
            Dialogs.dbError(Dialogs.ownerOf(totalValue), e);
            return;
        }
        Statistics s = ctx.statisticsService.calculate(views, LocalDate.now());

        totalValue.setText(String.valueOf(s.total()));
        sumValue.setText(Labels.money(s.totalBudget()));
        avgValue.setText(Labels.money(s.avgBudget()));
        minMaxValue.setText(s.total() == 0 ? "—" : Labels.money(s.minBudget()) + "\n" + Labels.money(s.maxBudget()));
        durationValue.setText(Labels.number(s.avgDuration(), 1) + " ч");
        upcomingValue.setText(String.valueOf(s.upcoming()));
        completedValue.setText(Labels.number(s.completedSharePercent(), 1) + " %");
        topLocationValue.setText(s.topLocation());

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Заявки");
        for (RequestStatus st : RequestStatus.values()) {
            series.getData().add(new XYChart.Data<>(Labels.status(st), s.byStatus().get(st)));
        }
        statusChart.getData().setAll(List.of(series));
        locationTable.setItems(FXCollections.observableArrayList(s.byLocation()));
    }
}
