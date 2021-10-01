package ch.epfl.biop.utils;

import ij.measure.ResultsTable;
import loci.plugins.LociImporter;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.model.ImageData;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;



public class SplitOmeroURL {

    public String host;
    public List<Long> imageIDs = new ArrayList<>();



    /**
     * retrieve Omero host from an OMERO URL
     * @param omeroURL
     * @throws MalformedURLException
     */
    public void setHost(String omeroURL) throws MalformedURLException {
        URL url = new URL(omeroURL);
        this.host = url.getHost();
    }

    /**
     * Set all OMERO image IDs from an OMERO dataset- or image- URL
     * Supported URLs include:
     *      - URLs generated from the "create link" button from OMERO.web's mainpage:
     *              - Single image, e.g:  "https://hostname/webclient/?show=image-4738"
     *              - Multiple images, e.g:  "https://hostname/webclient/?show=image-4736|image-4737|image-4738"
     *              - Single dataset, e.g:  "https://hostname/webclient/?show=dataset-604"
     *              - Multiple datasets, e.g:  "https://hostname/webclient/?show=dataset-604|dataset-603"
     *      - URLs pasted from the OMERO.iviewer
     *              - Single image opened with a double clic on a thumbnail, e.g:  "https://hostname/webclient/img_detail/4735/?dataset=604"
     *              - Single image opened with the "open with.. iviewer" button, e.g:  "https://hostname/iviewer/?images=4737&dataset=604"
     *              - Multiple images opened with the "open with.. iviewer" button, e.g:  "https://hostname/iviewer/?images=4736,4737,4738,4739"
     *
     * @param omeroURL OMERO dataset- or image- URL
     * @param gateway OMERO gateway
     * @param ctx OMERO security context
     * @throws ExecutionException
     * @throws DSOutOfServiceException
     * @throws DSAccessException
     * @throws MalformedURLException
     *
     */
    public void setImageIDs(String omeroURL, Gateway gateway, SecurityContext ctx) throws ExecutionException, DSOutOfServiceException, DSAccessException, MalformedURLException {

        URL url = new URL(omeroURL);
        String query = url.getQuery();

        // case single or multiple image(s) link, generated with the CREATE LINK BUTTON in OMERO.web
        // Single image example: https://hostname/webclient/?show=image-4738
        // Multiple images example: https://hostname/webclient/?show=image-4736|image-4737|image-4738
        if (query.contains("show=image-")) {
            String[] parts = query.split("-");
            // deal with links created while multiple images are selected
            for (int i = 1; i < parts.length; i++) {
                if (parts[i].contains("image")) {
                    String part = parts[i];
                    String[] subParts = part.split("\\|image");
                    this.imageIDs.add(Long.valueOf(subParts[0]));
                }
            }
            this.imageIDs.add(Long.valueOf(parts[parts.length - 1]));

        // case single or multiple dataset link, generated with the CREATE LINK BUTTON  in OMERO.web
        // Single dataset example: https://hostname/webclient/?show=dataset-604
        // Multiple datasets example: https://hostname/webclient/?show=dataset-604|dataset-603
        } else if(query.contains("show=dataset-")){
            String[] parts = query.split("-");
            for(int i = 1; i<parts.length; i++) {
                if (parts[i].contains("dataset")) {
                    String[] subParts = parts[i].split("\\|dataset");
                    long datasetID = Long.valueOf(subParts[0]);
                    BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
                    Collection<ImageData> images = browse.getImagesForDatasets(ctx, Arrays.asList(datasetID));
                    Iterator<ImageData> j = images.iterator();
                    ImageData image;
                    while (j.hasNext()) {
                        image = j.next();
                        this.imageIDs.add(image.getId());
                    }
                }
            }
            long datasetID = Long.valueOf(parts[parts.length-1]);
            BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
            Collection<ImageData> images = browse.getImagesForDatasets(ctx, Arrays.asList(datasetID));
            Iterator<ImageData> j = images.iterator();
            ImageData image;
            while (j.hasNext()) {
                image = j.next();
                this.imageIDs.add(image.getId());
            }

        // case single image link, pasted from iviewer (iviewer opened with a double clic on a thumbnail)
        // Example: https://hostname/webclient/img_detail/4735/?dataset=604
        } else if (omeroURL.contains("img_detail")) {
            String[] parts = url.getFile().split("/");
            int index = findIndexOfStringInStringArray(parts,"img_detail")+1;
            this.imageIDs.add(Long.valueOf(parts[index]));

        // case single or multiple image(s) link, pasted from iviewer (iviewer opened with the "open with.." option)
        // Single image example: https://hostname/iviewer/?images=4737&dataset=604
        // Multiple images example: https://hostname/iviewer/?images=4736,4737,4738,4739
        } else if (query.contains("images=")) {
            if (query.contains(",")){
                //multiple images link
                String[] parts = query.split(",");
                this.imageIDs.add(Long.valueOf(parts[0].substring(parts[0].indexOf("=")+1)));
                for(int i = 1; i<parts.length; i++){
                    this.imageIDs.add(Long.valueOf(parts[i]));
                }
            } else {
                //simple image link
                String[] parts = query.split("&");
                this.imageIDs.add(Long.valueOf(parts[0].substring(parts[0].indexOf("=")+1)));
            }
        } else {
            System.out.println("Invalid OMERO URL");
        }
    }

    public int findIndexOfStringInStringArray(String[] array, String pattern){
        int idx = 0;
        for(String content : array){
            if(StringUtils.contains(content, pattern)){
                return idx;
            }
            ++idx;
        }
        return -1;
    }
}
