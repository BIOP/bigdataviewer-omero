package ch.epfl.biop.ij2command;


import bdv.util.BdvFunctions;
import bdv.util.BdvStackSource;
import bdv.util.volatiles.VolatileViews;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.process.ImageProcessor;
import loci.plugins.LociImporter;
import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ImageData;
import omero.log.SimpleLogger;
import org.scijava.command.Command;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

//New class for displaying all images from an OMERO Dataset in a tiled manner in BDV
@Plugin(type = Command.class, menuPath = "Plugins>BIOP>OpenDataset")
public class OmeroOpenDatasetCommand implements Command {

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

    @Parameter(label = "Enter the ID of your OMERO dataset")
    long datasetID;

    static int port = 4064;

    // @Parameter
    // ImagePlus image;

    int index = 1;

    @Override
    public void run() {
        // Run the function
        // Connect to Omero
        // https://downloads.openmicroscopy.org/omero/5.4.10/api/omero/gateway/Gateway.html
        Gateway gateway = null;
        try {
            gateway = omeroConnect(host, port, username, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println( "Session active : "+gateway.isConnected() );
        System.out.println( "Disconnecting...");

        System.out.println( "Session active : "+gateway.isConnected() );

        ExperimenterData exp = gateway.getLoggedInUser();
        long groupID = exp.getGroupId();
        SecurityContext ctx = new SecurityContext(groupID);
        Collection<ImageData> images = null;
        try {
            images = getImagesFromDataset(gateway, ctx, datasetID);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        long[] total_dim = new long[2];
        total_dim[0] = 0;
        total_dim[1] = 0;

        //Iterator<ImageData> j = images.iterator();
        for (ImageData img : images){
            long imageID = img.getId();
            openImagePlus(host,username,password,imageID);
            ImagePlus image = WindowManager.getCurrentImage();
            final long imageOffset = image.getHeight();

            total_dim[0] = image.getWidth()*image.getNChannels();
            total_dim[1] = total_dim[1]+ imageOffset;

            // Create cached image factory of Type Byte
            ReadOnlyCachedCellImgOptions options = new ReadOnlyCachedCellImgOptions();
            // Put cell dimensions to image width and height
            options = options.cellDimensions(image.getWidth(),image.getHeight());
            final ReadOnlyCachedCellImgFactory factory = new ReadOnlyCachedCellImgFactory(options);



            UnsignedShortType t = new UnsignedShortType();

            CellLoader<UnsignedShortType> loader = new CellLoader<UnsignedShortType>(){
                @Override
                public void load(SingleCellArrayImg<UnsignedShortType, ?> singleCellArrayImg) throws Exception {

                    ImageProcessor ip = image.getStack().getProcessor(index);

                    long[] positions = new long[2];
                    Cursor<UnsignedShortType> cursor = singleCellArrayImg.localizingCursor();

                    final long channelOffset = - (index-1)*image.getWidth();

                    // move through pixels until there is no pixel left in this cell
                    while (cursor.hasNext())
                    {
                        // move the cursor forward by one pixel
                        cursor.fwd();
                        //get the current position
                        cursor.localize(positions);
                        long px = positions[0] + channelOffset;
                        long py = positions[1] + imageOffset;
                        //get pixel value of the input image (from stack) at pos (px,py) and copy it to the current cell at the same position
                        cursor.get().set(ip.getPixel((int) px,(int) py));
                    }
                    index = index+1;
                }
            };

            RandomAccessibleInterval<UnsignedShortType> randomAccessible = factory.create(total_dim, t,loader);
            //ask if pixel has already been loaded or not
            RandomAccessibleInterval volatilerandomAccessible = VolatileViews.wrapAsVolatile(randomAccessible);
            BdvStackSource bss = BdvFunctions.show(volatilerandomAccessible,"Tiling");
            bss.setDisplayRange(0, 1500);
            /*BdvOptions bdvOptions = new BdvOptions();
            bdvOptions.addTo(bss.getBdvHandle());
            AffineTransform3D transform3D = new AffineTransform3D();
            transform3D.rotate(2,Math.PI/2.0);
            bdvOptions.sourceTransform(transform3D);
            bss = BdvFunctions.show(volatilerandomAccessible,"Tiling",bdvOptions);
            */
            gateway.disconnect();
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


    void openImagePlus(String host,String username,String password,long imageID){
        String options = "";
        options += "location=[OMERO] open=[omero:server=";
        options += host;
        options += "\nuser=";
        options += username;
        options += "\npass=";
        options += password;
        //options += "\ngroupID=";
        //options += groupID;
        options += "\niid=";
        options += imageID;
        options += "]";
        //options += " use_virtual_stack";
        options += " windowless=true ";
        IJ.runPlugIn("loci.plugins.LociImporter",  options);
        LociImporter li = new LociImporter();
    }

    Collection<ImageData> getImagesFromDataset(Gateway gateway, SecurityContext ctx, long DatasetID) throws ExecutionException {
        //List all images contained in a Dataset
        BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
        Collection<Long> datasetIds = new ArrayList<>();
        datasetIds.add(new Long(DatasetID));
        try {
            return browse.getImagesForDatasets(ctx, datasetIds);
        } catch (DSOutOfServiceException e) {
            e.printStackTrace();
        } catch (DSAccessException e) {
            e.printStackTrace();
        }
        return null;
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
        //ij.command().run(OmeroOpenDatasetCommand.class, true);
    }


}