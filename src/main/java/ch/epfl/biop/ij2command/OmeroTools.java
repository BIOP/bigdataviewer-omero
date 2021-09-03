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
import omero.api.RawPixelsStorePrx;
import omero.api.ResolutionDescription;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.RawDataFacility;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ImageData;
import omero.gateway.model.PixelsData;
import omero.gateway.rnd.Plane2D;
import omero.log.SimpleLogger;
import omero.model.enums.UnitsLength;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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


    public static Collection<ImageData> getImagesFromDataset(Gateway gateway, long DatasetID) throws Exception{
        //List all images contained in a Dataset
        BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
        SecurityContext ctx = getSecurityContext(gateway);
        Collection<Long> datasetIds = new ArrayList<>();
        datasetIds.add(new Long(DatasetID));
        return browse.getImagesForDatasets(ctx, datasetIds);

    }

    public static SecurityContext getSecurityContext(Gateway gateway)throws Exception{
        ExperimenterData exp = gateway.getLoggedInUser();
        long groupID = exp.getGroupId();
        SecurityContext ctx = new SecurityContext(groupID);
        return ctx;
    }

    public static PixelsData getPixelsDataFromOmeroID(long imageID, Gateway gateway, SecurityContext ctx) throws Exception{

            BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
            ImageData image = browse.getImage(ctx, imageID);
            PixelsData pixels = image.getDefaultPixels();
            return pixels;

    }

}