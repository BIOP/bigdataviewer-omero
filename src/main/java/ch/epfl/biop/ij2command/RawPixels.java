package ch.epfl.biop.ij2command;

import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import omero.gateway.Gateway;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;




//New class for displaying all images from an OMERO Dataset in a tiled manner in BDV
@Plugin(type = Command.class, menuPath = "Plugins>BIOP>get raw pixels")
public class RawPixels implements Command {


    @Parameter(label = "OMERO host")
    String host;

    @Parameter(label = "Enter your gaspar username")
    String username;

    @Parameter(label = "Enter your gaspar password", style = "password", persist = false)
    String password;

    @Parameter(label = "Enter the ID of your OMERO image")
    long imageID;

    static int port = 4064;

    @Override
    public void run() {
        // https://downloads.openmicroscopy.org/omero/5.4.10/api/omero/gateway/Gateway.html
        try {
            Gateway gateway =  OmeroTools.omeroConnect(host, port, username, password);
            System.out.println( "Session active : "+gateway.isConnected() );
            RandomAccessibleInterval volatilerandomAccessible = OmeroTools.openRawPlaneRandomAccessibleInterval(gateway,imageID,true);
            gateway.disconnect();

            BdvStackSource bss = BdvFunctions.show(volatilerandomAccessible,"OMERO raw plane");
            bss.setDisplayRange(0, 1500);

        } catch (Exception e) {
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

        ij.command().run(RawPixels.class, true);
    }

}