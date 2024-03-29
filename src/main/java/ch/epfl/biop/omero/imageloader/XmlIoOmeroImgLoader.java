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
package ch.epfl.biop.omero.imageloader;
import ch.epfl.biop.ij2command.OmeroTools;
import ch.epfl.biop.omero.omerosource.OmeroSourceOpener;
import com.google.gson.Gson;
import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import org.jdom2.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;


@ImgLoaderIo( format = "spimreconstruction.biop_omeroimageloader", type = OmeroImageLoader.class )
public class XmlIoOmeroImgLoader implements XmlIoBasicImgLoader<OmeroImageLoader> {

    public static final String OPENER_CLASS_TAG = "opener_class";
    public static final String OPENER_TAG = "opener";
    public static final String CACHE_NUM_FETCHER = "num_fetcher_threads";
    public static final String CACHE_NUM_PRIORITIES = "num_priorities";
    public static final String DATASET_NUMBER_TAG = "dataset_number";

    Map<String, OmeroTools.GatewaySecurityContext> hostToGatewayCtx = new HashMap<String, OmeroTools.GatewaySecurityContext>();

    @Override
    public Element toXml(OmeroImageLoader imgLoader, File basePath) {
        final Element elem = new Element( "ImageLoader" );
        elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, this.getClass().getAnnotation( ImgLoaderIo.class ).format() );
        // For potential extensibility
        elem.addContent(XmlHelpers.textElement( OPENER_CLASS_TAG, OmeroSourceOpener.class.getName()));
        elem.addContent(XmlHelpers.intElement( CACHE_NUM_FETCHER, imgLoader.numFetcherThreads));
        elem.addContent(XmlHelpers.intElement( CACHE_NUM_PRIORITIES, imgLoader.numPriorities));
        elem.addContent(XmlHelpers.intElement( DATASET_NUMBER_TAG, imgLoader.openers.size()));

        Gson gson = new Gson();
        for (int i=0;i<imgLoader.openers.size();i++) {
            // Opener serialization
            elem.addContent(XmlHelpers.textElement(OPENER_TAG+"_"+i, gson.toJson(imgLoader.openers.get(i))));
        }
        return elem;
    }

    @Override
    public OmeroImageLoader fromXml(Element elem, File basePath, AbstractSequenceDescription<?, ?, ?> sequenceDescription) {
        try
        {
            final int number_of_datasets = XmlHelpers.getInt( elem, DATASET_NUMBER_TAG );
            final int numFetcherThreads = XmlHelpers.getInt(elem, CACHE_NUM_FETCHER);
            final int numPriorities = XmlHelpers.getInt(elem, CACHE_NUM_PRIORITIES);

            List<OmeroSourceOpener> openers = new ArrayList<>();

            String openerClassName = XmlHelpers.getText( elem, OPENER_CLASS_TAG );

            if (!openerClassName.equals(OmeroSourceOpener.class.getName())) {
                throw new UnsupportedOperationException("Error class "+openerClassName+" not recognized.");
            }

            //TODO handle login to OMERO
            int port = 4064;

            Gson gson = new Gson();
            for (int i=0;i<number_of_datasets;i++) {
                // Opener de-serialization
                String jsonInString = XmlHelpers.getText( elem, OPENER_TAG+"_"+i );
                OmeroSourceOpener opener = gson.fromJson(jsonInString, OmeroSourceOpener.class);

                if (!hostToGatewayCtx.containsKey(opener.getHost())) {

                    //Get credentials
                    Boolean onlyCredentials = false;
                    String[] credentials = OmeroTools.getOmeroConnectionInputParameters(onlyCredentials);
                    String username = credentials[0];
                    String password = credentials[1];
                    credentials = new String[]{};

                    // connect to OMERO
                    Gateway gateway =  OmeroTools.omeroConnect(opener.getHost(), port, username, password);
                    SecurityContext ctx = OmeroTools.getSecurityContext(gateway);

                    //add it in the channel hashmap
                    OmeroTools.GatewaySecurityContext gtCtx = new OmeroTools.GatewaySecurityContext(opener.getHost(), port, gateway,ctx);
                    hostToGatewayCtx.put(opener.getHost(),gtCtx);
                }
                opener.gateway(hostToGatewayCtx.get(opener.getHost()).gateway)
                        .securityContext(hostToGatewayCtx.get(opener.getHost()).ctx)
                        .create();
                openers.add(opener);
            }


            return new OmeroImageLoader(openers,sequenceDescription,numFetcherThreads, numPriorities);
        }
        catch ( final Exception e )
        {
            throw new RuntimeException( e );
        }
    }
}
