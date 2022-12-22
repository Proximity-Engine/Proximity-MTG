package dev.hephaestus.proximity.scryfall;

import dev.hephaestus.proximity.app.api.logging.Log;
import dev.hephaestus.proximity.app.api.plugins.DataProvider;
import dev.hephaestus.proximity.app.api.util.Task;
import dev.hephaestus.proximity.app.api.Proximity;
import dev.hephaestus.proximity.json.api.*;
import javafx.application.Platform;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CardNameTextField extends TextField {
  private final ContextMenu entriesPopup = new ContextMenu();
  private final Task task;

  private final Binding<String> stringBinding = Bindings.createStringBinding(this::getText);

  public CardNameTextField(DataProvider.Context context) {
    super();
    this.task = new AutocompleteTask(context.log());

    // The entire reason we need a custom skin here is to remove line 530 of ContextMenuContent which
    // selects whatever option you're on when you press space. Very annoying for a text field.
    this.entriesPopup.setSkin(new CustomContextMenuSkin(this.entriesPopup));

    this.setBackground(Background.EMPTY);
    this.setStyle("-fx-text-fill: #FFFFFFD0; -fx-prompt-text-fill: #FFFFFFA0;");
    this.setPromptText("Card Name");

    this.entriesPopup.prefWidthProperty().bind(this.widthProperty());

    textProperty().addListener((observableValue, oldValue, newValue) -> {
      if (this.getText().length() < 2) {
        this.entriesPopup.hide();
      } else {
        this.task.run();
      }
    });

    this.focusedProperty().addListener(observableValue -> this.entriesPopup.hide());
  }

  public ObservableValue<String> getAutoCompletionBinding() {
    return this.stringBinding;
  }

  private class AutocompleteTask extends Task {
    public AutocompleteTask(Log log) {
      super("autocomplete", log);
    }

    @Override
    protected Builder<?> addSteps(Builder<Void> builder) {
      return builder.then(this::getSuggestions)
              .then(this::createMenuItems)
              .then(this::setPopupItems);
    }

    private List<String> getSuggestions() {
      String userText = CardNameTextField.this.getText();

      try {
        URL url = Proximity.cache(URI.create(
                "https://api.scryfall.com/cards/autocomplete?q=" + URLEncoder.encode(userText, StandardCharsets.UTF_8) + "&include_extras=true"
        ).toURL());

        JsonObject object = Json.parseObject(new InputStreamReader(url.openStream()));
        JsonArray data = object.getArray("data");
        List<String> results = new ArrayList<>(data.size());

        for (JsonElement result : data) {
          results.add(((JsonString) result).get());
        }

        return results;
      } catch (IOException e) {
        this.log.print(e);

        return Collections.emptyList();
      }
    }

    private List<MenuItem> createMenuItems(List<String> suggestions) {
      List<MenuItem> menuItems = new ArrayList<>(suggestions.size());

      for (String result : suggestions) {
        MenuItem item = new MenuItem(result);

        item.setOnAction(event -> {
          CardNameTextField.this.setText(result);
          CardNameTextField.this.entriesPopup.hide();
          CardNameTextField.this.stringBinding.invalidate();
        });

        menuItems.add(item);
      }

      return menuItems;
    }

    private void setPopupItems(List<MenuItem> menuItems) {
      Platform.runLater(() -> {
        CardNameTextField.this.entriesPopup.getItems().clear();
        CardNameTextField.this.entriesPopup.getItems().addAll(menuItems);

        if (menuItems.size() == 1) {
          if (!menuItems.get(0).getText().equalsIgnoreCase(CardNameTextField.this.getText())) {
            CardNameTextField.this.setText(menuItems.get(0).getText());
            CardNameTextField.this.entriesPopup.hide();
            CardNameTextField.this.stringBinding.invalidate();
          }
        } else if (!CardNameTextField.this.entriesPopup.isShowing()) {
          CardNameTextField.this.entriesPopup.show(CardNameTextField.this, Side.BOTTOM, 0, 0);
        }
      });
    }
  }
}