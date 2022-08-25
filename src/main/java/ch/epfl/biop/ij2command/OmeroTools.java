/*-
 * #%L
 * A nice project implementing an OMERO connection with ImageJ
 * %%
 * Copyright (C) 2021 EPFL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package ch.epfl.biop.ij2command;

import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ImageData;
import omero.gateway.model.PixelsData;
import omero.log.SimpleLogger;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


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

   /* public static String[] getUserCredentials(){
        //Get username
        String username = (String) JOptionPane.showInputDialog(null, "Enter Your OMERO Username: ", null);

        // get password
        JPasswordField jpf = new JPasswordField(24);
        Box box = Box.createHorizontalBox();
        box.add(jpf);
        JOptionPane.showConfirmDialog(null, box, "Enter Your OMERO Password: ", JOptionPane.OK_CANCEL_OPTION);
        char[] chArray = jpf.getPassword();
        String password = new String(chArray);
        Arrays.fill(chArray, (char) 0);

        return new String[]{username,password};
    }*/

    public static String[] getOmeroConnectionInputParameters(boolean onlyCredentials){

        JTextField host = new JTextField("omero-server.epfl.ch",20);
        JSpinner port = new JSpinner();
        port.setValue(4064);
        JTextField username = new JTextField(50);
        JPasswordField jpf = new JPasswordField(24);

        JPanel myPanel = new JPanel();
        myPanel.setLayout(new BoxLayout(myPanel, BoxLayout.Y_AXIS));
        if(!onlyCredentials) {
            myPanel.add(new JLabel("host"));
            myPanel.add(host);
            myPanel.add(Box.createVerticalStrut(15)); // a spacer
            myPanel.add(new JLabel("port"));
            myPanel.add(port);
            myPanel.add(Box.createVerticalStrut(15)); // a spacer
        }

        myPanel.add(new JLabel("Username"));
        myPanel.add(username);
        myPanel.add(Box.createVerticalStrut(15)); // a spacer
        myPanel.add(new JLabel("Password"));
        myPanel.add(jpf);

        int result = JOptionPane.showConfirmDialog(null, myPanel,
                "Please enter OMERO connection input parameters", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            ArrayList<String> omeroParameters = new ArrayList<>();
            if(!onlyCredentials) {
                omeroParameters.add(host.getText());
                omeroParameters.add(port.getValue().toString());
            }
            omeroParameters.add(username.getText());
            char[] chArray = jpf.getPassword();
            omeroParameters.add(new String(chArray));
            Arrays.fill(chArray, (char) 0);

            String[] omeroParametersArray = new String[omeroParameters.size()];
            return omeroParameters.toArray(omeroParametersArray);
        }
        return  null;
    }

    public static class GatewaySecurityContext {
        public Gateway gateway;
        public SecurityContext ctx;
        public String host;
        public int port;

        public GatewaySecurityContext(String host, int port, Gateway gateway,SecurityContext ctx) {
            this.gateway = gateway;
            this.ctx = ctx;
            this.host = host;
            this.port = port;
        }
    }
}
