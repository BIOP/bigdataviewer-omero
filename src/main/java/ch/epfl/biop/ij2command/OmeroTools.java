package ch.epfl.biop.ij2command;

import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ImageData;
import omero.gateway.model.PixelsData;
import omero.log.SimpleLogger;

import java.util.ArrayList;
import java.util.Collection;


public class OmeroTools {

    /**
     * OMERO connection with credentials and simpleLogger
     * @param hostname OMERO Host name
     * @param port     Port (Usually 4064)
     * @param userName OMERO User
     * @param password Password for OMERO User
     * @return OMERO gateway (Gateway for simplifying access to an OMERO server)
     * @throws Exception
     */
    public static Gateway omeroConnect(String hostname, int port, String userName, String password) throws Exception {
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


    public static Collection<ImageData> getImagesFromDataset(Gateway gateway, long DatasetID) throws Exception {
        //List all images contained in a Dataset
        BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
        SecurityContext ctx = getSecurityContext(gateway);
        Collection<Long> datasetIds = new ArrayList<>();
        datasetIds.add(new Long(DatasetID));
        return browse.getImagesForDatasets(ctx, datasetIds);
    }

    /**
     * @param gateway OMERO gateway
     * @return Security context hosting information required to access correct connector
     * @throws Exception
     */
    public static SecurityContext getSecurityContext(Gateway gateway) throws Exception {
        ExperimenterData exp = gateway.getLoggedInUser();
        long groupID = exp.getGroupId();
        SecurityContext ctx = new SecurityContext(groupID);
        return ctx;
    }

    /**
     * @param imageID ID of the OMERO image to access
     * @param gateway OMERO gateway
     * @param ctx     OMERO Security context
     * @return OMERO raw pixel data
     * @throws Exception
     */
    public static PixelsData getPixelsDataFromOmeroID(long imageID, Gateway gateway, SecurityContext ctx) throws Exception {

        BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
        ImageData image = browse.getImage(ctx, imageID);
        PixelsData pixels = image.getDefaultPixels();
        return pixels;

    }

}