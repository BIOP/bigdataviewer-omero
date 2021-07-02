import ch.epfl.biop.ij2command.OmeroOpenImageCommand;
import net.imagej.ImageJ;
import org.junit.Assert;
import org.junit.Test;
import org.scijava.command.CommandModule;
import java.util.concurrent.Future;

public class OmeroOpenImageCommandTest {


    public void run() throws Exception {
        // Arrange
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();

        // Act
        Future<CommandModule> m = ij.command().run(OmeroOpenImageCommand.class, true, "number1", 2, "number2", 3);

        // Assert
        Assert.assertEquals(m.get().getOutput("the_answer_to_everything"), 42);
    }
}