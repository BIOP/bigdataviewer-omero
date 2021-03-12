package ch.epfl.biop.ij2command;


import net.imagej.ImageJ;
import omero.gateway.Gateway;
import omero.gateway.model.ImageData;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Collection;

//New class for displaying all images from an OMERO Dataset in a tiled manner in BDV
@Plugin(type = Command.class, menuPath = "Plugins>BIOP>OpenDataset")
public class OmeroOpenDatasetCommand implements Command {


    @Parameter(label = "OMERO host")
    String host;

    @Parameter(label = "Enter your gaspar username")
    String username;

    @Parameter(label = "Enter your gaspar password", style = "password", persist = false)
    String password;

    @Parameter(label = "Enter the ID of your OMERO dataset")
    long datasetID;

    static int port = 4064;

    int index = 1;

    @Override
    public void run() {
        // Run the function
        // Connect to Omero
        // https://downloads.openmicroscopy.org/omero/5.4.10/api/omero/gateway/Gateway.html
        try {
        Gateway gateway =  OmeroTools.omeroConnect(host, port, username, password);
        System.out.println( "Session active : "+gateway.isConnected() );
        Collection<ImageData> images = OmeroTools.getImagesFromDataset(gateway, datasetID);
        gateway.disconnect();

        /*
        Iterator<ImageData> j = images.iterator();
        ImageData image;
        while (j.hasNext()) {
            image = j.next();
            // Do something
        }*/

        for (ImageData img : images){
            long imageID = img.getId();


            //BdvStackSource bss = BdvFunctions.show(volatilerandomAccessible,"Tiling");
            //bss.setDisplayRange(0, 1500);
            /*BdvOptions bdvOptions = new BdvOptions();
            bdvOptions.addTo(bss.getBdvHandle());
            AffineTransform3D transform3D = new AffineTransform3D();
            transform3D.rotate(2,Math.PI/2.0);
            bdvOptions.sourceTransform(transform3D);
            bss = BdvFunctions.show(volatilerandomAccessible,"Tiling",bdvOptions);
            */

        }

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

        //ij.command().run(OmeroTilingCommand.class, true);
        ij.command().run(OmeroOpenDatasetCommand.class, true);
    }


}