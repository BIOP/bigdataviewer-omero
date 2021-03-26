package ch.epfl.biop.ij2command;

import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import net.imagej.ImageJ;
import net.imglib2.type.numeric.ARGBType;
import omero.gateway.Gateway;
import omero.gateway.model.PixelsData;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;


//New class for displaying an OMERO image (raw pixels) in 3D in BDV
@Plugin(type = Command.class, menuPath = "Plugins>BIOP>display raw pixels in 3D")
public class RawPixelsfromSource implements Command {


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
            PixelsData pixels = OmeroTools.getPixelsDataFromOmeroID(imageID,gateway);

            BdvStackSource bss = null;

            for (int c=0; c<pixels.getSizeC(); c++) {
                OmeroSource source = new OmeroSource(pixels.getSizeT(),c,pixels,gateway);
                bss = BdvFunctions.show(source);
                //bss = BdvFunctions.show(source,pixels.getSizeT());

                //add a time slider
                bss.getBdvHandle().getViewerPanel().setNumTimepoints(pixels.getSizeT());
                bss.setDisplayRange(0, 500);
                // Color : Random color for each channel
                bss.setColor(new ARGBType(ARGBType.rgba(255*Math.random(),255*Math.random(),255*Math.random(),1)));
            }

            gateway.disconnect();

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

        ij.command().run(RawPixelsfromSource.class, true);
    }

}