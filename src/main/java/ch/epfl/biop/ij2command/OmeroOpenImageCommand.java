package ch.epfl.biop.ij2command;

import loci.plugins.LociImporter;
import net.imagej.ImageJ;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.model.ExperimenterData;
import omero.model.IObject;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.IOException;


//import os.*;
//import os.path.*;

import ij.*;
import ij.IJ;

//import omero.gateway.LoginCredentials
//import omero.gateway.Gateway

import omero.gateway.*;
import omero.gateway.facility.BrowseFacility;

import omero.log.SimpleLogger;
import ome.formats.importer.*;
import ome.formats.importer.cli.*;
import loci.formats.in.DefaultMetadataOptions;
import loci.formats.in.MetadataLevel;

/**
 * This example illustrates how to create an ImageJ 2 {@link Command} plugin.
 * The pom file of this project is customized for the PTBIOP Organization (biop.epfl.ch)
 * <p>
 * The code here is opening the biop website. The command can be tested in the java DummyCommandTest class.
 * </p>
 */

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>OpenOmeroImage")
public class OmeroOpenImageCommand implements Command {

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

    //@Parameter(label = "Enter the ID of your OMERO dataset")
    //long datasetID;

    //@Parameter(label = "Enter the ID of your OMERO group")
    //long groupID;

    static int port = 4064;


    @Override
    public void run() {
        //uiService.show("Hello from the BIOP! Happy new year "+username+" !");
        // Connect to Omero
        // https://downloads.openmicroscopy.org/omero/5.4.10/api/omero/gateway/Gateway.html
        try {
            Gateway gateway = omeroConnect(host, port, username, password);
            System.out.println( "Session active : "+gateway.isConnected() );
            openImagePlus(host,username,password,imageID);
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
        //options += " windowless=true ";
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

        ij.command().run(OmeroOpenImageCommand.class, true);
    }

}