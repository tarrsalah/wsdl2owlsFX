/* 
 * The MIT License
 *
 * Copyright 2014 tarrsalah.org.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.tarrsalah.flycomp.wsdl2owlsfx.presentation.bone;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javax.inject.Inject;
import org.mindswap.owl.OWLFactory;
import org.mindswap.owl.OWLOntology;
import org.mindswap.owls.vocabulary.OWLS;
import org.mindswap.wsdl.WSDLOperation;
import org.mindswap.wsdl.WSDLTranslator;
import org.tarrsalah.flycomp.wsdl2owlsfx.App;
import org.tarrsalah.flycomp.wsdl2owlsfx.business.boundary.OperationsAsync;
import org.tarrsalah.flycomp.wsdl2owlsfx.business.model.ParamFactory;
import org.tarrsalah.flycomp.wsdl2owlsfx.business.model.Parameter;
import org.tarrsalah.flycomp.wsdl2owlsfx.presentation.ViewUtils;
import org.tarrsalah.flycomp.wsdl2owlsfx.presentation.editor.EditorPresenter;
import org.tarrsalah.flycomp.wsdl2owlsfx.presentation.editor.EditorView;

/**
 * FXML Controller class
 *
 * @author tarrsalah.org
 */
public class BonePresenter implements Initializable {

	private static final Logger LOG = Logger.getLogger(BonePresenter.class.getName());

	@FXML
	private TabPane tabPane;

	@FXML
	private Tab generatorTab;

	@FXML
	private MenuItem importWsdl, exit;

	@FXML
	private TextField wsdlURL;

	@FXML
	private Button viewOWLS;

	@FXML
	private TextField serviceName;

	@FXML
	private TextArea description;

	@FXML
	private TextField logicalURI;

	@FXML
	private ComboBox<WSDLOperation> services;

	@FXML
	private TableView<Parameter> inputs;

	@FXML
	private TableView<Parameter> outputs;

	@FXML
	private TableView namespaces;

	@FXML
	private TableColumn namespaceAbbr;

	@FXML
	private TableColumn namespaceURL;

	@FXML
	private ProgressIndicator progress;

	private final Map<WSDLOperation, ObservableList<Parameter>> currentInputsMap;
	private final Map<WSDLOperation, ObservableList<Parameter>> currentOutputsMap;

	@Inject
	private OperationsAsync operationsAsync;

	public BonePresenter() {
		currentInputsMap = new HashMap<>();
		currentOutputsMap = new HashMap<>();

	}

	/**
	 * Initializes the controller class.
	 *
	 * @param url
	 * @param rb
	 */
	@Override
	public void initialize(URL url, ResourceBundle rb) {

		generatorTab.setClosable(false);
		importWsdl.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCodeCombination.CONTROL_DOWN));
		exit.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCodeCombination.CONTROL_DOWN));
//		viewOWLS.setDisable(true);
		viewOWLS.disableProperty().bind(services.valueProperty().isNull());

		operationsAsync.setExecutor(App.executor);
		operationsAsync.wsdlURLProperty().bind(wsdlURL.textProperty());
		operationsAsync.setOnSucceeded(this::handleOperationsAsyncSuccess);
		operationsAsync.setOnFailed(this::handleOperationsAsyncFail);
		ViewUtils.bindWorkerToProgressIndicator(operationsAsync, progress);

		Stream.of(inputs, outputs).forEach(
				tableView -> {
					tableView.getColumns().forEach((TableColumn c) -> {
						c.setCellFactory(TextFieldTableCell.<Parameter>forTableColumn());
					});
				}
		);

		namespaceAbbr.prefWidthProperty().bind(namespaces.widthProperty().multiply(0.25));
		namespaceURL.prefWidthProperty().bind(namespaces.widthProperty().multiply(0.75));

		services.setConverter(new StringConverter<WSDLOperation>() {
			@Override
			public String toString(WSDLOperation operation) {
				return operation.getName();
			}

			@Override
			public WSDLOperation fromString(String name) {
				return services.getItems().stream()
						.filter(service
								-> (service.getName() == null
								? name == null
								: service.getName().equals(name)))
						.findFirst()
						.get();
			}
		});
	}

	private void addEditorTab(String title, File file) {
		Tab owlsEditor = new Tab(title);
		final EditorView editorView = new EditorView();
		((EditorPresenter) editorView.getPresenter()).showFileContent(title, file);
		final HBox box = (HBox) editorView.getView();
		// dirty hack!
		box.prefHeightProperty().bind(tabPane.heightProperty());
		box.prefWidthProperty().bind(tabPane.widthProperty());
		owlsEditor.setContent(box);

		tabPane.getTabs().add(owlsEditor);
		tabPane.getSelectionModel().select(owlsEditor); //select by object
	}

	private synchronized void putWSDLOperation(WSDLOperation operation) {
		currentInputsMap.put(operation, ParamFactory.getInputParams(operation));
		currentOutputsMap.put(operation, ParamFactory.getOutputParams(operation));
	}

	private synchronized void clearWSDLOperation() {
		currentInputsMap.clear();
		currentOutputsMap.clear();
	}

	private void handleOperationsAsyncSuccess(WorkerStateEvent event) {
		List<WSDLOperation> fetchedOperations = operationsAsync.getValue();
		fetchedOperations.forEach(this::putWSDLOperation);
		services.setItems(FXCollections.observableArrayList(fetchedOperations));
		fetchedOperations.stream().findFirst().ifPresent(services::setValue);
//		viewOWLS.setDisable(false);
		handleServices();
		LOG.log(Level.INFO,
				() -> {
					return String.join(" ", "Fetching from",
							operationsAsync.getWsdlURL(),
							" succeeded");
				});
	}

	@SuppressWarnings("ThrowableResultIgnored")
	private void handleOperationsAsyncFail(WorkerStateEvent event) {
		services.getItems().clear();
		clearWSDLOperation();
//		viewOWLS.setDisable(true);
		LOG.warning(
				() -> {
					return "Fetcher Service task failed "
					+ operationsAsync.getException().getMessage();

				});
	}

	@FXML
	private void handleClose() {
		Platform.exit();
	}

	@FXML
	private void handleImportWsdl() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Choose a WSDL file");
		fileChooser.getExtensionFilters().addAll(new ExtensionFilter("WSDL Files", "*.wsdl"));
		File selectedFile = fileChooser.showOpenDialog(
				(Stage) generatorTab.getTabPane().getParent().getScene().getWindow());
		if (Objects.nonNull(selectedFile)) {
			wsdlURL.setText(selectedFile.toURI().toString());
			handleWsdlUrl();
		}
	}

	@FXML
	private void handleViewOwls() {
		try {
			File file = File.createTempFile("_" + String.valueOf(System.currentTimeMillis()), "owls");
			file.deleteOnExit();

			final OWLOntology ontology = OWLFactory.createKB().createOntology(URI.create(logicalURI.getText()));
			OWLS.addOWLSImports(ontology);

			final WSDLOperation operation = services.getSelectionModel().getSelectedItem();
			final String name = serviceName.getText().trim();

			final WSDLTranslator translator = new WSDLTranslator(ontology,
					operation, name);
			translator.setServiceName(name);
			translator.setTextDescription(description.getText().trim());

			// TODO: eliminate duplication -> move the lambda to the Parameter  class.
			inputs.getItems().forEach(input -> {
				translator.addInput(input.getInitialParameter(),
						input.getWsdlName(),
						URI.create("http://www.jiji.org"),
						input.getXslt());
			});

			outputs.getItems().forEach(output -> {
				translator.addOutput(output.getInitialParameter(),
						output.getWsdlName(),
						URI.create("http://www.jiji.org"),
						output.getXslt());
			});
			translator.writeOWLS(new FileOutputStream(file));
			this.addEditorTab(name, file);

		} catch (IOException ex) {
			LOG.log(Level.SEVERE, ex.getMessage());
		}
	}

	@FXML
	private void handleWsdlUrl() {
		switch (operationsAsync.getState()) {
			case READY:
				operationsAsync.start();
				break;
			default:
				operationsAsync.restart();
		}
	}

	@FXML
	private void handleServices() {
		SingleSelectionModel<WSDLOperation> currentOpeation
				= services.selectionModelProperty().getValue();
		if (currentOpeation.isEmpty()) {
			serviceName.clear();
			description.clear();
			inputs.getItems().clear();
			outputs.getItems().clear();

		} else {
			final WSDLOperation operation = currentOpeation.getSelectedItem();
			serviceName.setText(Objects.requireNonNull(operation.getName(),
					"Default name"));
			description.setText(Objects.requireNonNull(operation.getDescription(),
					"Default description"));
			inputs.setItems(currentInputsMap.get(operation));
			outputs.setItems(currentOutputsMap.get(operation));
		}
		Stream.of(inputs, outputs).forEach(ViewUtils::refreshTableView);
	}
}
