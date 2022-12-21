package dev.hephaestus.proximity.scryfall;

import dev.hephaestus.proximity.app.api.Option;
import dev.hephaestus.proximity.app.api.Proximity;
import dev.hephaestus.proximity.app.api.RenderJob;
import dev.hephaestus.proximity.app.api.logging.ExceptionUtil;
import dev.hephaestus.proximity.app.api.logging.Log;
import dev.hephaestus.proximity.app.api.plugins.DataProvider;
import dev.hephaestus.proximity.app.api.plugins.DataWidget;
import dev.hephaestus.proximity.app.api.util.Task;
import dev.hephaestus.proximity.json.api.Json;
import dev.hephaestus.proximity.json.api.JsonElement;
import dev.hephaestus.proximity.json.api.JsonObject;
import dev.hephaestus.proximity.mtg.cards.BaseMagicCard;
import dev.hephaestus.proximity.mtg.cards.MagicCard;
import dev.hephaestus.proximity.mtg.cards.SingleFacedCard;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ScryfallDataWidget extends DataWidget<BaseMagicCard> {
    private final VBox root = new VBox();
    private final Selector selector;

    public ScryfallDataWidget(DataProvider.Context context) {
        super(context);
        this.selector = new Selector();
        this.root.getChildren().add(this.selector);
    }

    @Override
    public Pane getRootPane() {
        return this.root;
    }

    @Override
    public JsonElement saveState() {
        JsonObject.Mutable json = JsonObject.create();

        json.put("name", this.selector.cardName.getText());

        return json;
    }

    private void select(MouseEvent observable) {
        Proximity.select(this);
    }

    public void select(JsonObject json) {
        this.selector.disable();

        this.selector.cardName.setText(json.getString("name"));
        this.selector.setCode.setValue(json.getString("set").toUpperCase(Locale.ROOT));
        this.selector.collectorNumber.setValue(json.getString("collector_number").toUpperCase(Locale.ROOT));
        this.selector.setCode.setDisable(false);
        this.selector.collectorNumber.setDisable(false);
        this.selector.populateQuietly.run();
        this.selector.select(json);

        this.selector.enable();
    }

    private class Selector extends HBox {
        private final CardNameTextField cardName;
        private final ComboBox<String> setCode;
        private final ComboBox<String> collectorNumber;
        private final Task populate;
        private final Task populateQuietly;

        private Map<String, List<JsonObject>> printings = Collections.emptyMap();
        private final ChangeListener<String> updateListener = this::update;
        private final ChangeListener<String> setChangeListener = this::changeSet;

        private EventHandler<MouseEvent> listener = ScryfallDataWidget.this::select;
        private JsonObject selected;

        Selector() {
            this.cardName = new CardNameTextField(ScryfallDataWidget.this.context);

            HBox.setHgrow(this.cardName, Priority.ALWAYS);

            this.setCode = new ComboBox<>();
            setCode.setDisable(true);

            this.collectorNumber = new ComboBox<>();
            collectorNumber.setDisable(true);

            this.getChildren().addAll(cardName, setCode, collectorNumber);

            this.cardName.getAutoCompletionBinding().addListener(this::populate);

            this.enable();

            this.setCode.setPrefWidth(150);
            this.collectorNumber.setPrefWidth(150);

            this.populate = new PopulatePrintingsTask(ScryfallDataWidget.this.context.log(), false);
            this.populateQuietly = new PopulatePrintingsTask(ScryfallDataWidget.this.context.log(), true);

            this.addListeners();
        }

        private void invokeListener(MouseEvent event) {
            if (this.listener != null) {
                this.listener.handle(event);
            }
        }

        private void addListeners() {
            this.setOnMouseClicked(this::invokeListener);
            this.cardName.setOnMouseClicked(this::invokeListener);
            this.setCode.setOnMouseClicked(this::invokeListener);
            this.collectorNumber.setOnMouseClicked(this::invokeListener);
        }

        private void populate(Observable observable) {
            this.populate.run();
        }

        private void changeSet(Observable observable, String oldValue, String newValue) {
            this.collectorNumber.getItems().clear();

            if (newValue != null && this.printings.containsKey(newValue)) {
                for (JsonObject printing : this.printings.get(newValue)) {
                    this.disable();
                    this.collectorNumber.getItems().add(printing.getString("collector_number").toUpperCase(Locale.ROOT));
                    this.collectorNumber.setValue(printing.getString("collector_number").toUpperCase(Locale.ROOT));
                    this.enable();
                }
            }
        }

        private void enable() {
            this.setCode.valueProperty().addListener(this.setChangeListener);
            this.setCode.valueProperty().addListener(this.updateListener);
            this.collectorNumber.valueProperty().addListener(this.updateListener);
        }

        private void disable() {
            this.setCode.valueProperty().removeListener(this.setChangeListener);
            this.setCode.valueProperty().removeListener(this.updateListener);
            this.collectorNumber.valueProperty().removeListener(this.updateListener);
        }

        private void update(Observable observable, String oldValue, String newValue) {
            if (oldValue != null && oldValue.equals(newValue)) return;

            String setCode = this.setCode.getValue();
            String collectorNumber = this.collectorNumber.getValue();

            if (setCode == null && collectorNumber == null) {
                JsonObject mostRecentPrinting = this.printings.values().iterator().next().get(0);
                String set = mostRecentPrinting.getString("set").toUpperCase(Locale.ROOT);

                this.disable();
                this.setCode.setValue(set);
                this.collectorNumber.itemsProperty().set(this.printings.get(set).stream().map(json -> json.getString("collector_number").toUpperCase(Locale.ROOT)).collect(Collectors.toCollection(FXCollections::observableArrayList)));
                this.collectorNumber.setValue(mostRecentPrinting.getString("collector_number").toUpperCase(Locale.ROOT));
                this.enable();

                this.setCode.setDisable(false);
                this.collectorNumber.setDisable(false);

                this.select(mostRecentPrinting);
            } else {
                for (JsonObject printing : this.printings.get(setCode.toUpperCase(Locale.ROOT))) {
                    if (printing.getString("collector_number").equalsIgnoreCase(collectorNumber)) {
                        if (this.selected == null || !selected.equals(printing)) {
                            this.select(printing);
                        }

                        break;
                    }
                }
            }
        }

        private void select(JsonObject json) {
            if (this.selected != null && json.getString("id").equals(this.selected.getString("id"))) return;

            List<BaseMagicCard> cards = MagicCard.parse(json, ScryfallDataWidget.this.context.log());
            ObservableList<Entry> currentValue = ScryfallDataWidget.this.entries.getValue();

            if (cards.size() == 1) {
                if (currentValue.size() == 1) {
                    copyOptions(currentValue.get(0).get(), cards.get(0));
                }

                ScryfallDataWidget.this.entries.set(FXCollections.singletonObservableList(new SingleEntry(this, cards.get(0))));
            } else if (!cards.isEmpty()) {
                ObservableList<Entry> list = FXCollections.observableArrayList();
                List<Node> nodes = new ArrayList<>();

                list.add(new MultiEntry(cards.get(0)));

                nodes.add(this);
                nodes.add(list.get(0).getRootPane());

                for (int i = 1; i < cards.size(); ++i) {
                    MultiEntry entry = new MultiEntry(cards.get(i));
                    list.add(entry);
                    nodes.add(entry.getRootPane());
                }

                if (currentValue.size() == list.size()) {
                    for (int i = 0; i < currentValue.size(); ++i) {
                        copyOptions(currentValue.get(i).get(), list.get(i).get());
                    }
                } else if (currentValue.size() == 1) {
                    for (var entry : list) {
                        copyOptions(currentValue.get(0).get(), entry.get());
                    }
                }

                ScryfallDataWidget.this.getRootPane().getChildren().setAll(nodes);
                ScryfallDataWidget.this.entries.set(list);
            }

            this.requestFocus();

            this.selected = json;
        }

        private static void copyOptions(RenderJob<?> job1, RenderJob<?> job2) {
            for (var option : job1.options()) {
                copyOption(option, job1, job2);
            }
        }

        private static <T> void copyOption(Option<T, ?, ?> option, RenderJob<?> job1, RenderJob<?> job2) {
            job2.getOptionProperty(option).setValue(job1.getOption(option));
        }

        private void populatePrintings(boolean update) {
            if (!ScryfallDataWidget.this.errors.isEmpty()) {
                ScryfallDataWidget.this.errors.clear();
            }

            try {
                URL url = Proximity.cache(URI.create(
                        "https://api.scryfall.com/cards/search?order=released&unique=prints&q=" + URLEncoder.encode("!\"" + this.cardName.getText() + "\" include:extras", StandardCharsets.UTF_8)
                ).toURL());

                JsonObject printingJson = Json.parseObject(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));

                this.printings.clear();

                for (JsonElement card : printingJson.getArray("data")) {
                    this.printings.computeIfAbsent(card.asObject().getString("set").toUpperCase(Locale.ROOT), s -> new ArrayList<>(1))
                            .add(card.asObject());
                }

                while (printingJson.getBoolean("has_more")) {
                    url = Proximity.cache(URI.create(printingJson.getString("next_page")).toURL());

                    printingJson = Json.parseObject(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));

                    for (JsonElement card : printingJson.getArray("data")) {
                        this.printings.computeIfAbsent(card.asObject().getString("set").toUpperCase(Locale.ROOT), s -> new ArrayList<>(1))
                                .add(card.asObject());
                    }
                }

                Platform.runLater(() -> {
                    this.setCode.getItems().clear();

                    for (String setCode : this.printings.keySet()) {
                        this.setCode.getItems().add(setCode.toUpperCase(Locale.ROOT));
                    }

                    if (update) {
                        this.update(null, null, null);
                    }
                });
            } catch (FileNotFoundException e) {
                // Almost always a 404 response from Scryfall
                Platform.runLater(() -> {
                    ScryfallDataWidget.this.errors.add(String.format("Card '%s' not found.", this.cardName.getText()));
                    Selector.this.listener = ScryfallDataWidget.this::select;
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    ScryfallDataWidget.this.errors.add(ExceptionUtil.getErrorMessage(e));
                    ScryfallDataWidget.this.context.log().print(e);
                    ScryfallDataWidget.this.entries.clear();
                    this.setCode.setDisable(true);
                    this.setCode.getItems().clear();
                    this.collectorNumber.setDisable(true);
                    this.collectorNumber.getItems().clear();
                    Selector.this.listener = ScryfallDataWidget.this::select;
                });
            }
        }

        private class PopulatePrintingsTask extends Task {
            private final boolean quiet;

            public PopulatePrintingsTask(Log log, boolean quiet) {
                super("populate-printings", log);
                this.quiet = quiet;
            }

            @Override
            protected Builder<?> addSteps(Builder<Void> builder) {
                return builder.then(this::init)
                        .then(this::fetchPrintings)
                        .then(this::complete);
            }

            private void init() {
                if (!ScryfallDataWidget.this.errors.isEmpty()) {
                    ScryfallDataWidget.this.errors.clear();
                }
            }

            private Map<String, List<JsonObject>> fetchPrintings() {
                try {
                    URL url = Proximity.cache(URI.create(
                            "https://api.scryfall.com/cards/search?order=released&unique=prints&q=" + URLEncoder.encode("!\"" + Selector.this.cardName.getText() + "\" include:extras", StandardCharsets.UTF_8)
                    ).toURL());

                    JsonObject printingJson = Json.parseObject(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));

                    Map<String, List<JsonObject>> printings = new LinkedHashMap<>();

                    for (JsonElement card : printingJson.getArray("data")) {
                        printings.computeIfAbsent(card.asObject().getString("set").toUpperCase(Locale.ROOT), s -> new ArrayList<>(1))
                                .add(card.asObject());
                    }

                    while (printingJson.getBoolean("has_more")) {
                        url = Proximity.cache(URI.create(printingJson.getString("next_page")).toURL());

                        printingJson = Json.parseObject(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));

                        for (JsonElement card : printingJson.getArray("data")) {
                            printings.computeIfAbsent(card.asObject().getString("set").toUpperCase(Locale.ROOT), s -> new ArrayList<>(1))
                                    .add(card.asObject());
                        }
                    }

                    return printings;
                } catch (FileNotFoundException e) {
                    // Almost always a 404 response from Scryfall
                    Platform.runLater(() -> {
                        ScryfallDataWidget.this.errors.add(String.format("Card '%s' not found.", Selector.this.cardName.getText()));
                        Selector.this.listener = ScryfallDataWidget.this::select;
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        ScryfallDataWidget.this.errors.add(ExceptionUtil.getErrorMessage(e));
                        ScryfallDataWidget.this.context.log().print(e);
                        ScryfallDataWidget.this.entries.clear();
                        Selector.this.setCode.setDisable(true);
                        Selector.this.setCode.getItems().clear();
                        Selector.this.collectorNumber.setDisable(true);
                        Selector.this.collectorNumber.getItems().clear();
                        Selector.this.listener = ScryfallDataWidget.this::select;
                    });
                }

                return Collections.emptyMap();
            }

            private void complete(Map<String, List<JsonObject>> printings) {
                Selector.this.printings = printings;

                Platform.runLater(() -> {
                    Selector.this.setCode.getItems().clear();

                    for (String setCode : printings.keySet()) {
                        Selector.this.setCode.getItems().add(setCode.toUpperCase(Locale.ROOT));
                    }

                    if (!this.quiet) {
                        Selector.this.update(null, null, null);
                    }
                });
            }
        }
    }
}
