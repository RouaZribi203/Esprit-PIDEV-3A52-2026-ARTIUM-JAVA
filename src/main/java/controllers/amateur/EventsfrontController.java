package controllers.amateur;

import entities.Evenement;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import services.EvenementService;

import java.io.IOException;
import java.sql.SQLDataException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import controllers.amateur.EventCardController;

public class EventsfrontController {

	@FXML
	private TextField searchField;

	@FXML
	private ToggleButton allTabButton;

	@FXML
	private ToggleButton expositionTabButton;

	@FXML
	private ToggleButton concertTabButton;

	@FXML
	private ToggleButton spectacleTabButton;

	@FXML
	private ToggleButton conferenceTabButton;

	@FXML
	private FlowPane eventsFlowPane;

	@FXML
	private Label emptyStateLabel;

	private final EvenementService evenementService = new EvenementService();
	private final List<Evenement> allEvents = new ArrayList<>();
	private String selectedCategory = "Tous";
	private Consumer<Evenement> detailNavigationHandler;

	@FXML
	public void initialize() {
		refreshEvents();
		onAllTabClick();
	}

	public void setDetailNavigationHandler(Consumer<Evenement> detailNavigationHandler) {
		this.detailNavigationHandler = detailNavigationHandler;
		// Cards are first rendered during initialize(), before parent injects navigation handler.
		// Re-apply filters so every visible card receives the click callback.
		if (!allEvents.isEmpty()) {
			applyFilters();
		}
	}

	@FXML
	private void onSearchClick() {
		applyFilters();
	}

	@FXML
	private void onAllTabClick() {
		selectedCategory = "Tous";
		setActiveTab(allTabButton);
		applyFilters();
	}

	@FXML
	private void onExpositionTabClick() {
		selectedCategory = "Exposition";
		setActiveTab(expositionTabButton);
		applyFilters();
	}

	@FXML
	private void onConcertTabClick() {
		selectedCategory = "Concert";
		setActiveTab(concertTabButton);
		applyFilters();
	}

	@FXML
	private void onSpectacleTabClick() {
		selectedCategory = "Spectacle";
		setActiveTab(spectacleTabButton);
		applyFilters();
	}

	@FXML
	private void onConferenceTabClick() {
		selectedCategory = "Conférence";
		setActiveTab(conferenceTabButton);
		applyFilters();
	}

	private void refreshEvents() {
		try {
			allEvents.clear();
			allEvents.addAll(evenementService.getAll());
			allEvents.sort(Comparator.comparing(Evenement::getDateDebut, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
		} catch (SQLDataException e) {
			emptyStateLabel.setText("Impossible de charger les evenements.");
			emptyStateLabel.setVisible(true);
			emptyStateLabel.setManaged(true);
		}
	}

	private void applyFilters() {
		String search = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

		List<Evenement> filtered = allEvents.stream()
				.filter(event -> matchesCategory(event, selectedCategory))
				.filter(event -> matchesSearch(event, search))
				.collect(Collectors.toList());

		renderCards(filtered);
	}

	private boolean matchesCategory(Evenement event, String category) {
		if (category == null || "Tous".equalsIgnoreCase(category)) {
			return true;
		}
		return category.equalsIgnoreCase(safe(event.getType()));
	}

	private boolean matchesSearch(Evenement event, String search) {
		if (search.isEmpty()) {
			return true;
		}

		return contains(event.getTitre(), search)
				|| contains(event.getDescription(), search)
				|| contains(event.getType(), search)
				|| contains(event.getStatut(), search)
				|| contains(event.getPrixTicket() == null ? null : String.valueOf(event.getPrixTicket()), search);
	}

	private void renderCards(List<Evenement> events) {
		eventsFlowPane.getChildren().clear();

		for (Evenement event : events) {
			try {
				FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/views/amateur/components/event-card.fxml")));
				Parent card = loader.load();
				controllers.amateur.EventCardController controller = loader.getController();
				controller.setData(event);
				controller.setDetailHandler(detailNavigationHandler);
				eventsFlowPane.getChildren().add(card);
			} catch (IOException e) {
				emptyStateLabel.setText("Erreur lors de l'affichage des evenements.");
				emptyStateLabel.setVisible(true);
				emptyStateLabel.setManaged(true);
				return;
			}
		}

		boolean empty = events.isEmpty();
		emptyStateLabel.setVisible(empty);
		emptyStateLabel.setManaged(empty);
	}

	private void setActiveTab(ToggleButton activeButton) {
		ToggleButton[] buttons = {allTabButton, expositionTabButton, concertTabButton, spectacleTabButton, conferenceTabButton};
		for (ToggleButton button : buttons) {
			if (button != null) {
				button.getStyleClass().remove("active");
				button.setSelected(false);
			}
		}
		if (activeButton != null) {
			activeButton.getStyleClass().add("active");
			activeButton.setSelected(true);
		}
	}

	private boolean contains(String value, String search) {
		return value != null && value.toLowerCase().contains(search);
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}
}



