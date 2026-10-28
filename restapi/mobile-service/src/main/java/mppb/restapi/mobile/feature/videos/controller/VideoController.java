package mppb.restapi.mobile.feature.videos.controller;

import mppb.restapi.common.infrastructure.util.Constants;
import mppb.restapi.mobile.feature.videos.dto.VideosResponseDto;
import mppb.restapi.mobile.feature.videos.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/" + Constants.API_PREFIX + "/public/videos")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @GetMapping
    public VideosResponseDto getVideos() {
        return videoService.getVideos();
    }
}
