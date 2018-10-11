package ch.epfl.biop.ij2command;

import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.IOException;
import java.net.URL;

/**
 * This example illustrates how to create an ImageJ 2 {@link Command} plugin.
 * The pom file of this project is customized for the PTBIOP Organization (biop.epfl.ch)
 * <p>
 * The code here is a simple Gaussian blur using ImageJ Ops.
 * </p>
 */
@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Dummy Command")
public class DummyCommand implements Command {

    @Parameter
    private UIService uiService;

    @Parameter
    PlatformService ps;

    @Override
    public void run() {
        uiService.show("Hello from the BIOP!");
        try {
            ps.open(new URL("http://biop.epfl.ch"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ij.command().run(DummyCommand.class, true);
    }

}
