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
package org.tarrsalah.flycomp.wsdl2owlsfx.presentation.editor;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebView;

public class EditorPresenter implements Initializable {

    private static final Logger LOG = Logger.getLogger(EditorPresenter.class.getName());
    private final String JS_FILE = "file:///home/tarrsalah/src/github.com/tarrsalah/wsdl2owlsFX/src/main/resources/prettify.js";
    private final String CSS_FILE = "file:///home/tarrsalah/src/github.com/tarrsalah/wsdl2owlsFX/src/main/resources/prettify.css";

    @FXML
    private WebView editor;

    private final String template
            = "<!doctype html>"
            + "<html>"
            + "<head>"
            + "<style>"
            + "${css}"
            + "</style>"
            + "<script>"
            + "${js}"
            + "</script>"
            + "</head>"
            + "<body onload=\"prettyPrint()\">"
            + "<pre class=\"prettyprint lang-xml\">"
            + "<code>"
            + "${content}"
            + "</code>"
            + "</pre>"
            + "</body>"
            + "</html>";

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    // Not Thread safe
    // Must be invoked from the javafx application thread
    public void showFileContent(String title, File file) {
        try {
            Optional<String> owls = Files.lines(file.toPath(), Charset.forName("UTF-8"))
                    .reduce((line1, line2) -> line1 + "\n" + line2);
            owls.ifPresent(html -> editor.getEngine().loadContent(parse(html)));
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, ex.getMessage());
        }
    }

    private String parse(String owls) {
        try {
            return template.replace("${content}", owls.replace("<", "&lt;")
                    .replace(">", "&gt;"))
                    .replace("${js}",
                            Files.lines(Paths.get(URI.create(JS_FILE)
                                    ))
                            .reduce((line1, line2) -> line1 + "\n" + line2).orElse(""))
                    .replace("${css}",
                            Files.lines(Paths.get(URI.create(CSS_FILE)
                                    ))
                            .reduce((line1, line2) -> line1 + "\n" + line2).orElse(""));
        } catch (IOException ex) {
            Logger.getLogger(EditorPresenter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
