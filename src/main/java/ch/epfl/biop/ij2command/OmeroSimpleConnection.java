package ch.epfl.biop.ij2command;

import net.imagej.ImageJ;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.log.SimpleLogger;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;



@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Omero connect")
public class OmeroSimpleConnection implements Command {


    @Parameter(label = "OMERO host")
    String host;

    @Parameter(label = "Enter your gaspar username")
    String username;

    @Parameter(label = "Enter your gaspar password", style = "password", persist = false)
    String password;

    static int port = 4064;

    @Override
    public void run() {
        try {
            Gateway gateway =  omeroConnect(host, port, username, password);
            System.out.println( "Session active : "+gateway.isConnected() );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

        ij.command().run(OmeroSimpleConnection.class, true);

    }

}