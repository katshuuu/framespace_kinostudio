package ru.mirea.kinostudio.fx.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import ru.mirea.kinostudio.exception.BusinessException;
import ru.mirea.kinostudio.exception.EntityNotFoundException;
import ru.mirea.kinostudio.fx.AppContext;
import ru.mirea.kinostudio.fx.Converters;
import ru.mirea.kinostudio.fx.Dialogs;
import ru.mirea.kinostudio.fx.FormDialogs;
import ru.mirea.kinostudio.model.Location;
import ru.mirea.kinostudio.model.LocationType;
import ru.mirea.kinostudio.model.RequestView;
import ru.mirea.kinostudio.util.Labels;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Вкладка «Локации»: список, поиск, фильтр по типу, сортировка, CRUD и заявки на выбранной локации. */
public class LocationsController {

    private enum SortOption {
        NAME("Название: А → Я", Comparator.comparing((Location l) -> l.getName().toLowerCase(Locale.ROOT))),
        CAPACITY_DESC("Вместимость: по убыванию", Comparator.comparingInt(Location::getCapacity).reversed()),
        CAPACITY_ASC("Вместимость: по возрастанию", Comparator.comparingInt(Location::getCapacity));

        final String label;
        final Comparator<Location> comparator;

        SortOption(String label, Comparator<Location> comparator) {
            this.label = label;
            this.comparator = comparator;
        }
    }

    private final AppContext ctx = AppContext.get();

    private final ObservableList<Location> all = FXCollections.observableArrayList();
    private final FilteredList<Location> filtered = new FilteredList<>(all, l -> true);
    private final SortedList<Location> sorted = new SortedList<>(filtered);
    private final ObservableList<RequestView> locationRequests = FXCollections.observableArrayList();
    private Map<Integer, List<RequestView>> requestsByLocation = new HashMap<>();

    @FXML private TextField searchField;
    @FXML private ComboBox<LocationType> typeFilter;
    @FXML private ComboBox<SortOption> sortBox;
    @FXML private Label countLabel;
    @FXML private TableView<Location> table;
    @FXML private TableColumn<Location, Integer> idCol;
    @FXML private TableColumn<Location, String> nameCol;
    @FXML private TableColumn<Location, String> typeCol;
    @FXML private TableColumn<Location, String> addressCol;
    @FXML private TableColumn<Location, Integer> capacityCol;
    @FXML private TableColumn<Location, Integer> countCol;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Label relatedTitle;
    @FXML private TableView<RequestView> relatedTable;
    @FXML private TableColumn<RequestView, String> rTitleCol;
    @FXML private TableColumn<RequestView, String> rClientCol;
    @FXML private TableColumn<RequestView, String> rDateCol;
    @FXML private TableColumn<RequestView, String> rStatusCol;

    @FXML
    private void initialize() {
        idCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(Labels.type(c.getValue().getType())));
        addressCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAddress()));
        capacityCol.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getCapacity()));
        countCol.setCellValueFactory(c -> new SimpleObjectProperty<>(
                requestsByLocation.getOrDefault(c.getValue().getId(), List.of()).size()));
        capacityCol.getStyleClass().add("right");
        countCol.getStyleClass().add("right");
        table.getColumns().forEach(c -> c.setSortable(false));
        table.setItems(sorted);
        table.setPlaceholder(new Label("Локации не найдены"));
        table.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2
                    && table.getSelectionModel().getSelectedItem() != null) {
                onEdit();
            }
        });

        rTitleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        rClientCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getClientName()));
        rDateCol.setCellValueFactory(c -> new SimpleStringProperty(Labels.date(c.getValue().getShootingDate())));
        rStatusCol.setCellValueFactory(c -> new SimpleStringProperty(Labels.status(c.getValue().getStatus())));
        relatedTable.getColumns().forEach(c -> c.setSortable(false));
        relatedTable.setItems(locationRequests);
        relatedTable.setPlaceholder(new Label("На этой локации нет заявок"));

        typeFilter.setItems(FXCollections.observableArrayList(LocationType.values()));
        typeFilter.setConverter(Converters.locationType());
        sortBox.setItems(FXCollections.observableArrayList(SortOption.values()));
        sortBox.setConverter(Converters.of(o -> o.label));
        sortBox.valueProperty().addListener((o, a, b) -> sorted.setComparator(b == null ? null : b.comparator));
        sortBox.setValue(SortOption.NAME);

        searchField.textProperty().addListener((o, a, b) -> applyFilter());
        typeFilter.valueProperty().addListener((o, a, b) -> applyFilter());
        filtered.addListener((javafx.collections.ListChangeListener<Location>) c -> updateCount());

        var none = Bindings.isNull(table.getSelectionModel().selectedItemProperty());
        editButton.disableProperty().bind(none);
        deleteButton.disableProperty().bind(none);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showRelated(b));
        showRelated(null);

        ctx.addListener(this::reload);
        reload();
    }

    private void reload() {
        Location sel = table.getSelectionModel().getSelectedItem();
        Integer keepId = sel == null ? null : sel.getId();
        try {
            List<RequestView> views = ctx.viewService.loadAll();
            requestsByLocation = new HashMap<>();
            for (RequestView v : views) {
                requestsByLocation.computeIfAbsent(v.getRequest().getLocationId(), k -> new java.util.ArrayList<>()).add(v);
            }
            all.setAll(ctx.locationService.getAll());
        } catch (SQLException e) {
            Dialogs.dbError(Dialogs.ownerOf(table), e);
            return;
        }
        table.refresh();
        updateCount();
        if (keepId != null) {
            sorted.stream().filter(l -> l.getId() == keepId).findFirst()
                    .ifPresent(l -> table.getSelectionModel().select(l));
        }
        showRelated(table.getSelectionModel().getSelectedItem());
    }

    private void applyFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        LocationType type = typeFilter.getValue();
        filtered.setPredicate(l -> (type == null || l.getType() == type)
                && (q.isEmpty()
                || l.getName().toLowerCase(Locale.ROOT).contains(q)
                || l.getAddress().toLowerCase(Locale.ROOT).contains(q)));
    }

    private void updateCount() {
        countLabel.setText("Показано " + filtered.size() + " из " + all.size());
    }

    private void showRelated(Location l) {
        if (l == null) {
            relatedTitle.setText("Заявки на локации (выберите локацию)");
            locationRequests.clear();
            return;
        }
        relatedTitle.setText("Заявки на локации: " + l.getName());
        locationRequests.setAll(requestsByLocation.getOrDefault(l.getId(), List.of()));
    }

    @FXML
    private void onReset() {
        searchField.clear();
        typeFilter.setValue(null);
        sortBox.setValue(SortOption.NAME);
    }

    @FXML
    private void onAdd() {
        if (FormDialogs.<LocationDialogController>show(Dialogs.ownerOf(table), "Новая локация",
                "/fxml/location-dialog.fxml", d -> d.setLocation(null))) {
            ctx.dataChanged();
        }
    }

    @FXML
    private void onEdit() {
        Location l = table.getSelectionModel().getSelectedItem();
        if (l == null) return;
        if (FormDialogs.<LocationDialogController>show(Dialogs.ownerOf(table), "Редактирование локации",
                "/fxml/location-dialog.fxml", d -> d.setLocation(l))) {
            ctx.dataChanged();
        }
    }

    @FXML
    private void onDelete() {
        Location l = table.getSelectionModel().getSelectedItem();
        if (l == null) return;
        if (!Dialogs.confirm(Dialogs.ownerOf(table),
                "Удалить локацию «" + l.getName() + "»?\nДействие нельзя отменить.", "Удалить")) {
            return;
        }
        try {
            ctx.locationService.delete(l.getId());
            ctx.dataChanged();
        } catch (BusinessException e) {
            Dialogs.warning(Dialogs.ownerOf(table), e.getMessage());
        } catch (EntityNotFoundException e) {
            Dialogs.error(Dialogs.ownerOf(table), e.getMessage());
            ctx.dataChanged();
        } catch (SQLException e) {
            Dialogs.dbError(Dialogs.ownerOf(table), e);
        }
    }
}
