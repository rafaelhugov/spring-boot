package mppb.restapi.mobile.feature.videos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideosResponseDto {
    private VideoDto liveAtiva;
    private List<VideoDto> videos;
    private int total;
}
