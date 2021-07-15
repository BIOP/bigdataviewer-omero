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
        } else if (OmeroURL.contains("?show=image-")){
            String[] parts = OmeroURL.split("-");
            this.imageIDs.add(Long.valueOf(parts[parts.length-1]));
        } else if (OmeroURL.contains("img_detail")){
            String[] parts = OmeroURL.split("/");
            this.imageIDs.add(Long.valueOf(parts[parts.length-1]));
        } else {
            System.out.println("Invalid OMERO URL");
        }
        return this;
    }

}
