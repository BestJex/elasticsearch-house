package com.bubble.house.service.house;

import com.bubble.house.base.ToolKits;
import com.bubble.house.entity.dto.HouseDTO;
import com.bubble.house.entity.dto.HouseDetailDTO;
import com.bubble.house.entity.dto.HousePictureDTO;
import com.bubble.house.entity.house.*;
import com.bubble.house.entity.param.HouseParam;
import com.bubble.house.entity.param.PhotoParam;
import com.bubble.house.entity.result.ResultEntity;
import com.bubble.house.repository.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * House相关服务接口实现
 *
 * @author wugang
 * date: 2019-11-05 18:28
 **/
@Service
public class HouseServiceImpl implements HouseService {

    @Value("${qiniu.cdn.prefix}")
    private String cdnPrefix;
    private ModelMapper modelMapper;

    @PostConstruct
    public void init() {
        modelMapper = new ModelMapper();
    }

    private final SubwayRepository subwayRepository;
    private final SubwayStationRepository subwayStationRepository;
    private final HouseRepository houseRepository;
    private final HouseDetailRepository houseDetailRepository;
    private final HouseTagRepository houseTagRepository;
    private final HousePictureRepository housePictureRepository;

    public HouseServiceImpl(SubwayRepository subwayRepository, SubwayStationRepository subwayStationRepository,
                            HouseRepository houseRepository, HouseDetailRepository houseDetailRepository,
                            HouseTagRepository houseTagRepository, HousePictureRepository housePictureRepository) {
        this.subwayRepository = subwayRepository;
        this.subwayStationRepository = subwayStationRepository;
        this.houseRepository = houseRepository;
        this.houseDetailRepository = houseDetailRepository;
        this.houseTagRepository = houseTagRepository;
        this.housePictureRepository = housePictureRepository;
    }

    @Override
    public ResultEntity<HouseDTO> save(HouseParam houseParam) {
        HouseDetailEntity detail = new HouseDetailEntity();
        ResultEntity<HouseDTO> subwayValidationResult = wrapperDetailInfo(detail, houseParam);
        if (subwayValidationResult != null) {
            return subwayValidationResult;
        }

        HouseEntity house = new HouseEntity();
        this.modelMapper.map(houseParam, house);

        Date now = new Date();
        house.setCreateTime(now);
        house.setLastUpdateTime(now);
        // 当前操作用户ID
        house.setAdminId(ToolKits.getLoginUserId());
        house = this.houseRepository.save(house);

        detail.setHouseId(house.getId());
        detail = this.houseDetailRepository.save(detail);

        List<HousePictureEntity> pictures = generatePictures(houseParam, house.getId());
        Iterable<HousePictureEntity> housePictures = this.housePictureRepository.saveAll(pictures);

        HouseDTO houseDTO = this.modelMapper.map(house, HouseDTO.class);
        HouseDetailDTO houseDetailDTO = this.modelMapper.map(detail, HouseDetailDTO.class);

        houseDTO.setHouseDetail(houseDetailDTO);

        List<HousePictureDTO> pictureDTOS = new ArrayList<>();
        housePictures.forEach(housePicture -> pictureDTOS.add(this.modelMapper.map(housePicture, HousePictureDTO.class)));
        houseDTO.setPictures(pictureDTOS);
        houseDTO.setCover(this.cdnPrefix + houseDTO.getCover());

        List<String> tags = houseParam.getTags();
        if (tags != null && !tags.isEmpty()) {
            List<HouseTagEntity> houseTags = new ArrayList<>();
            for (String tag : tags) {
                houseTags.add(new HouseTagEntity(house.getId(), tag));
            }
            this.houseTagRepository.saveAll(houseTags);
            houseDTO.setTags(tags);
        }

        return new ResultEntity<>(true, null, houseDTO);
    }

    /**
     * 图片对象列表信息填充
     */
    private List<HousePictureEntity> generatePictures(HouseParam houseParam, Long houseId) {
        List<HousePictureEntity> pictures = new ArrayList<>();
        if (houseParam.getPhotos() == null || houseParam.getPhotos().isEmpty()) {
            return pictures;
        }
        for (PhotoParam photoParam : houseParam.getPhotos()) {
            HousePictureEntity picture = new HousePictureEntity();
            picture.setHouseId(houseId);
            picture.setCdnPrefix(this.cdnPrefix);
            picture.setPath(photoParam.getPath());
            picture.setWidth(photoParam.getWidth());
            picture.setHeight(photoParam.getHeight());
            pictures.add(picture);
        }
        return pictures;
    }

    /**
     * 房源详细信息对象填充
     */
    private ResultEntity<HouseDTO> wrapperDetailInfo(HouseDetailEntity houseDetail, HouseParam houseParam) {
        Optional<SubwayEntity> subwayOp = this.subwayRepository.findById(houseParam.getSubwayLineId());
        if (subwayOp.isPresent()) {
            SubwayEntity subway = subwayOp.get();
            Optional<SubwayStationEntity> subwayStationOp = this.subwayStationRepository.findById(houseParam.getSubwayStationId());
            if (!subwayStationOp.isPresent() || !subway.getId().equals(subwayStationOp.get().getSubwayId())) {
                return new ResultEntity<>(false, "Not valid subway station!");
            } else {
                SubwayStationEntity subwayStation = subwayStationOp.get();

                houseDetail.setSubwayLineId(subway.getId());
                houseDetail.setSubwayLineName(subway.getName());

                houseDetail.setSubwayStationId(subwayStation.getId());
                houseDetail.setSubwayStationName(subwayStation.getName());

                houseDetail.setDescription(houseParam.getDescription());
                houseDetail.setDetailAddress(houseParam.getDetailAddress());
                houseDetail.setLayoutDesc(houseParam.getLayoutDesc());
                houseDetail.setRentWay(houseParam.getRentWay());
                houseDetail.setRoundService(houseParam.getRoundService());
                houseDetail.setTraffic(houseParam.getTraffic());
            }
        } else {
            return new ResultEntity<>(false, "Not valid subway line!");
        }
        return null;
    }

}