package ch.epfl.biop.utils;


import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.model.ImageData;
import omero.model.Image;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class SplitOmeroURL {
    /*
    Dataset:

    https://omero-poc.epfl.ch/webclient/?show=dataset-1111
    or
    omero-poc.epfl.ch/webclient/?show=dataset-1111
    Image:
    https://omero-poc.epfl.ch/webclient/img_detail/3713/ (omero-poc.epfl.ch/webclient/img_detail/3713/)
    or
    https://omero-poc.epfl.ch/webclient/?show=image-3713 (omero-poc.epfl.ch/webclient/?show=image-3713)

     */

    public String host;
    public List<Long> imageIDs = new ArrayList<>();

    public int keywordIsMemberOf(String[] array, String pattern){
        int idx = 0;
        for(String content : array){
            if(StringUtils.contains(content, pattern)){
                return idx;
            }
            ++idx;
        }
        return -1;
    }

    // retrieve Omero host
    public SplitOmeroURL setHost(String OmeroURL) {
        String[] parts = OmeroURL.split("/");
        if(OmeroURL.contains("//")){
            this.host = parts[2];
        } else {
            this.host = parts[0];
        }
        return this;
    }

    // retrieve image IDs from dataset or image URL
    public SplitOmeroURL setImageIDs(String OmeroURL, Gateway gateway, SecurityContext ctx) throws ExecutionException, DSOutOfServiceException, DSAccessException {
        // case dataset link, generated with the CREATE LINK BUTTON  in OMERO.web
        // Example: https://omero-poc.epfl.ch/webclient/?show=dataset-604
        if(OmeroURL.contains("?show=dataset-")){
            String[] parts = OmeroURL.split("-");
            long datasetID = Long.valueOf(parts[parts.length-1]);
            BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
            Collection<ImageData> images = browse.getImagesForDatasets(ctx, Arrays.asList(datasetID));
            Iterator<ImageData> j = images.iterator();
            ImageData image;
            while (j.hasNext()) {
                image = j.next();
                this.imageIDs.add(image.getId());
            }
        // case single or multiple image(s) link, generated with the CREATE LINK BUTTON in OMERO.web
        // Single image example: https://omero-poc.epfl.ch/webclient/?show=image-4738
        // Multiple images example: https://omero-poc.epfl.ch/webclient/?show=image-4736|image-4737|image-4738|
        } else if (OmeroURL.contains("?show=image-")){
            String[] images = OmeroURL.split("show=image");
            String[] parts = images[1].split("-");
            // deal with links created while multiple images are selected
            for(int i = 1; i<parts.length; i++){
                if(parts[i].contains("image")) {
                    String part = parts[i];
                    String[] subParts = part.split("image");
                    this.imageIDs.add(Long.valueOf(subParts[0].substring(0, subParts[0].length() - 1)));
                }
            }
            this.imageIDs.add(Long.valueOf(parts[parts.length-1]));
            System.out.println("added IDs : " + imageIDs);
        // case single image link, pasted from iviewer (iviewer opened with a double clic on a thumbnail)
        // Example: https://omero-poc.epfl.ch/webclient/img_detail/4735/?dataset=604
        } else if (OmeroURL.contains("img_detail")) {
            String[] parts = OmeroURL.split("/");
            this.imageIDs.add(Long.valueOf(parts[parts.length - 1]));
        // case single or multiple image(s) link, pasted from iviewer (iviewer opened with the "open with.." option)
        // Single image example: https://omero-poc.epfl.ch/iviewer/?images=4737&dataset=604
        // Multiple images example: https://omero-poc.epfl.ch/iviewer/?images=4736,4737,4738,4739
        } else if (OmeroURL.contains("?images=")) {
            System.out.println("This type of OMERO URL is not supported yet. Please generate your link using the CREATE LINK BUTTON from OMERO.web");
        } else {
            System.out.println("Invalid OMERO URL");
        }
        return this;
    }

}
