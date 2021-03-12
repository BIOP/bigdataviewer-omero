package ch.epfl.biop.ij2command;

import bdv.util.volatiles.VolatileViews;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.process.ImageProcessor;
import loci.plugins.LociImporter;
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
import omero.gateway.facility.BrowseFacility;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ImageData;
import omero.log.SimpleLogger;

import java.util.ArrayList;
import java.util.Collection;

public class OmeroTools {


    public static Gateway omeroConnect(String hostname, int port, String userName, String password)throws Exception{
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



    public static ImagePlus openImagePlus(String host, String username, String password, long imageID, boolean windowless){
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
        if(windowless) {
            options += " windowless=true ";
        }
        IJ.runPlugIn("loci.plugins.LociImporter",  options);
        LociImporter li = new LociImporter();
        ImagePlus image = WindowManager.getCurrentImage();
        return image;
    }


    public static Collection<ImageData> getImagesFromDataset(Gateway gateway, long DatasetID) throws Exception{
        //List all images contained in a Dataset

        BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
        ExperimenterData exp = gateway.getLoggedInUser();
        long groupID = exp.getGroupId();
        SecurityContext ctx = new SecurityContext(groupID);
        Collection<Long> datasetIds = new ArrayList<>();
        datasetIds.add(new Long(DatasetID));
        return browse.getImagesForDatasets(ctx, datasetIds);

    }

    public static RandomAccessibleInterval openRandomAccessibleInterval(String host, String username, String password, long imageID, boolean windowless){
        ImagePlus image = OmeroTools.openImagePlus(host,username,password,imageID, windowless);

        long[] total_dim = new long[2];
        total_dim[0] = image.getWidth()*image.getNChannels();
        total_dim[1] = image.getHeight();

        // Create cached image factory of Type Byte
        ReadOnlyCachedCellImgOptions options = new ReadOnlyCachedCellImgOptions();
        // Put cell dimensions to image width and height
        options = options.cellDimensions(image.getWidth(),image.getHeight());
        final ReadOnlyCachedCellImgFactory factory = new ReadOnlyCachedCellImgFactory(options);

        UnsignedShortType t = new UnsignedShortType();

        CellLoader<UnsignedShortType> loader = new CellLoader<UnsignedShortType>(){
            @Override
            public void load(SingleCellArrayImg<UnsignedShortType, ?> singleCellArrayImg) throws Exception {
                long[] positions = new long[2];
                Cursor<UnsignedShortType> cursor = singleCellArrayImg.localizingCursor();
                cursor.localize(positions);
                int index = (int) ((positions[0]+1)/image.getWidth() + 1);
                ImageProcessor ip = image.getStack().getProcessor(index);
                final long channelOffset = - (index-1)*image.getWidth();

                // move through pixels until there is no pixel left in this cell
                while (cursor.hasNext())
                {
                    // move the cursor forward by one pixel
                    cursor.fwd();
                    //get the current position
                    cursor.localize(positions);
                    long px = positions[0] + channelOffset;
                    long py = positions[1];
                    //get pixel value of the input image (from stack) at pos (px,py) and copy it to the current cell at the same position
                    cursor.get().set(ip.getPixel((int) px,(int) py));
                }
            }

        };

        RandomAccessibleInterval<UnsignedShortType> randomAccessible = factory.create(total_dim, t,loader);
        //ask if pixel has already been loaded or not
        return VolatileViews.wrapAsVolatile(randomAccessible);
    }
}
