package com.ciff.app.convertor;

import com.ciff.app.dto.DemoItemCreateRequest;
import com.ciff.app.dto.DemoItemUpdateRequest;
import com.ciff.app.dto.DemoItemVO;
import com.ciff.app.entity.DemoItemPO;

import java.util.List;

public final class DemoItemConvertor {

    private DemoItemConvertor() {
    }

    public static DemoItemPO toPO(DemoItemCreateRequest request) {
        DemoItemPO po = new DemoItemPO();
        po.setName(request.getName());
        po.setStatus(request.getStatus());
        return po;
    }

    public static void updatePO(DemoItemPO po, DemoItemUpdateRequest request) {
        if (request.getName() != null) {
            po.setName(request.getName());
        }
        if (request.getStatus() != null) {
            po.setStatus(request.getStatus());
        }
    }

    public static DemoItemVO toVO(DemoItemPO po) {
        DemoItemVO vo = new DemoItemVO();
        vo.setId(po.getId());
        vo.setName(po.getName());
        vo.setStatus(po.getStatus());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }

    public static List<DemoItemVO> toVOList(List<DemoItemPO> pos) {
        return pos.stream().map(DemoItemConvertor::toVO).toList();
    }
}
