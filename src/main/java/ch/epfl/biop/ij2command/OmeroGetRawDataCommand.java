package ch.epfl.biop.ij2command;

import ij.IJ;
import net.imagej.ImageJ;
import omero.api.RawPixelsStorePrx;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.RawDataFacility;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ImageData;
import omero.gateway.model.PixelsData;
import omero.gateway.rnd.Plane2D;
import omero.log.Logger;
import omero.log.SimpleLogger;
import omero.model.IObject;
import omero.util.ReadOnlyByteArray;
import org.scijava.command.Command;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 *
 * https://docs.openmicroscopy.org/omero/5.6.3/developers/Java.html
 */

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Get Raw Omero Data")
public class OmeroGetRawDataCommand implements Command {

    @Parameter
    UIService uiService;

    @Parameter
    PlatformService ps;

    @Parameter(label = "OMERO host")
    String host;

    @Parameter(label = "Enter your gaspar username")
    String username;

    @Parameter(label = "Enter your gaspar password", style = "password", persist = false)
    String password;

    @Parameter(label = "Enter the ID of your OMERO image")
    long imageID;

    @Parameter(label = "Enter the ID of your OMERO dataset")
    long datasetID;

    @Parameter(label = "Enter the ID of your OMERO group")
    long groupID;

    static int port = 4064;

    @Parameter
    int x,y;


    @Override
    public void run() {
        //uiService.show("Hello from the BIOP! Happy new year "+username+" !");
        // Connect to Omero
        // https://downloads.openmicroscopy.org/omero/5.4.10/api/omero/gateway/Gateway.html
        try {
            //Gateway gateway = omeroConnect(host, port, username, password);

            LoginCredentials cred = new LoginCredentials(username, password, host, port);

            //to System.out or System.err
            Logger simpleLogger = new SimpleLogger();

            Gateway gateway = new Gateway(simpleLogger);
            ExperimenterData user = gateway.connect(cred);


            System.out.println( "Session active : "+gateway.isConnected() );

            //for every subsequent call to the server you'll need the
            //SecurityContext for a certain group; in this case create
            //a SecurityContext for the user's default group.
            SecurityContext ctx = new SecurityContext(user.getGroupId());

            BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
            ImageData image = browse.getImage(ctx, imageID);

            byte[] bytes  = new byte[500];
            ReadOnlyByteArray readOnlyByteArray = new ReadOnlyByteArray(bytes, 0,500);

            try (RawDataFacility rdf = gateway.getFacility(RawDataFacility.class)) {
                PixelsData pixels = image.getDefaultPixels();
                int sizeZ = pixels.getSizeZ();
                int sizeT = pixels.getSizeT();
                int sizeC = pixels.getSizeC();

                Plane2D p;
                /*for (int z = 0; z < sizeZ; z++)
                    for (int t = 0; t < sizeT; t++)
                        for (int c = 0; c < sizeC; c++) {

                        }*/

                p = rdf.getPlane(ctx, pixels, 0, 0, 0);
                p.getPixelValue(x,y);
                System.out.println("pix v = "+p.getPixelValue(x,y));
            }


            //------------------- OK

            PixelsData pixels = image.getDefaultPixels();
            long pixelsId = pixels.getId();
//offset values in each dimension XYZCT
            List<Integer> offset = new ArrayList<Integer>();
            int n = 5;
            for (int i = 0; i < n; i++) {
                offset.add(i, 0);
            }

            List<Integer> size = new ArrayList<Integer>();
            size.add(pixels.getSizeX());
            size.add(pixels.getSizeY());
            size.add(pixels.getSizeZ());
            size.add(pixels.getSizeC());
            size.add(pixels.getSizeT());

//indicate the step in each direction, step = 1,
//will return values at index 0, 1, 2.
//step = 2, values at index 0, 2, 4 etc.
            List<Integer> step = new ArrayList<Integer>();
            for (int i = 0; i < n; i++) {
                step.add(i, 1);
            }
            RawPixelsStorePrx store = null;
            try {
                store = gateway.getPixelsStore(ctx);
                store.setPixelsId(pixelsId, false);
                byte[] values = store.getHypercube(offset, size, step);
                //Do something
            } finally {
                store.close();
            }


            //openImagePlus(host,username,password,groupID,imageID);
            System.out.println( "Disconnecting...");
            gateway.disconnect();
            System.out.println( "Session active : "+gateway.isConnected() );
        }
        catch(Exception e) { e.printStackTrace();
        }

    }



    Gateway omeroConnect(String hostname, int port, String userName, String password)throws Exception{

        //Omero Connect with credentials and simpleLogger
        LoginCredentials cred = new LoginCredentials();
        cred.getServer().setHost(hostname);
        cred.getServer().setPort(port);
        cred.getUser().setUsername(userName);
        cred.getUser().setPassword(password);
        SimpleLogger simpleLogger = new SimpleLogger();
        Gateway gateway = new Gateway(simpleLogger);
        gateway.connect(cred);
        return gateway;
    }


    IObject find_dataset(Gateway gateway, long datasetID) throws Exception{
        //"Load the Dataset"
        BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
        ExperimenterData user = gateway.getLoggedInUser();
        SecurityContext ctx = new SecurityContext(user.getGroupId());
        return browse.findIObject( ctx, "omero.model.Dataset", datasetID);
    }

    void openImagePlus(String host,String username,String password,long groupID,long imageID){

        String options = "";
        options += "location=[OMERO] open=[omero:server=";
        options += host;
        options += "\nuser=";
        options += username;
        options += "\npass=";
        options += password;
        options += "\ngroupID=";
        options += groupID;
        options += "\niid=";
        options += imageID;
        options += "]";
        //options += " use_virtual_stack";
        options += " windowless=true ";

        IJ.runPlugIn("loci.plugins.LociImporter",  options);

        // LociImporter li = new LociImporter();


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

        ij.command().run(OmeroGetRawDataCommand.class, true);
    }

}