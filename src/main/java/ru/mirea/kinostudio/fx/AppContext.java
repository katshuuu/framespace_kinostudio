package ru.mirea.kinostudio.fx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import ru.mirea.kinostudio.model.Client;
import ru.mirea.kinostudio.model.Location;
import ru.mirea.kinostudio.repository.ClientRepository;
import ru.mirea.kinostudio.repository.LocationRepository;
import ru.mirea.kinostudio.repository.ShootingRequestRepository;
import ru.mirea.kinostudio.service.ClientService;
import ru.mirea.kinostudio.service.LocationService;
import ru.mirea.kinostudio.service.RequestViewService;
import ru.mirea.kinostudio.service.ShootingRequestService;
import ru.mirea.kinostudio.service.StatisticsService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Общий контекст приложения: здесь создаются сервисы и хранятся списки клиентов и локаций,
 * которые нужны сразу нескольким вкладкам (выпадающие списки, фильтры).
 *
 * Когда данные меняются (добавили, изменили, удалили), вызывается dataChanged():
 * контекст перечитывает справочники и уведомляет все вкладки — они обновляют свои таблицы.
 */
public final class AppContext {

    private static final AppContext INSTANCE = new AppContext();

    public final ClientService clientService;
    public final LocationService locationService;
    public final ShootingRequestService requestService;
    public final RequestViewService viewService;
    public final StatisticsService statisticsService = new StatisticsService();

    private final ObservableList<Client> clients = FXCollections.observableArrayList();
    private final ObservableList<Location> locations = FXCollections.observableArrayList();
    private final List<Runnable> listeners = new ArrayList<>();

    private AppContext() {
        ClientRepository clientRepo = new ClientRepository();
        LocationRepository locationRepo = new LocationRepository();
        ShootingRequestRepository requestRepo = new ShootingRequestRepository();
        clientService = new ClientService(clientRepo);
        locationService = new LocationService(locationRepo);
        requestService = new ShootingRequestService(requestRepo, clientRepo, locationRepo);
        viewService = new RequestViewService(requestService, clientService, locationService);
    }

    public static AppContext get() {
        return INSTANCE;
    }

    public ObservableList<Client> clients() {
        return clients;
    }

    public ObservableList<Location> locations() {
        return locations;
    }

    public void reloadReferences() throws SQLException {
        clients.setAll(clientService.getAll());
        locations.setAll(locationService.getAll());
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    /** Данные изменились: обновить справочники и все вкладки. */
    public void dataChanged() {
        try {
            reloadReferences();
        } catch (SQLException e) {
            Dialogs.dbError(null, e);
            return;
        }
        new ArrayList<>(listeners).forEach(Runnable::run);
    }
}
