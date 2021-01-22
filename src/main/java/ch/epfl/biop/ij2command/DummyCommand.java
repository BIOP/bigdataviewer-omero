package ch.epfl.biop.ij2command;

import net.imagej.ImageJ;

import org.scijava.ItemIO;
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
 * The code here is opening the biop website. The command can be tested in the java DummyCommandTest class.
 * </p>
 */

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Dummy Command")
public class DummyCommand implements Command {

    @Parameter
    UIService uiService;

    @Parameter
    PlatformService ps;

    @Parameter
    int number1;

    @Parameter
    int number2;

    @Parameter(label = "What is your nickname?")
    String name;

    @Parameter(type = ItemIO.OUTPUT)
    int the_answer_to_everything;

    @Override
    public void run() {
        uiService.show("Hello from the BIOP! Happy new year "+name+" !");
        try {
            ps.open(new URL("https://biop.epfl.ch"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        the_answer_to_everything = 42;
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
