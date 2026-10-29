package com.securepay.modules.layout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securepay.common.response.ResponseFactory;
import com.securepay.common.web.BaseController;
import com.securepay.modules.layout.entity.SideNav;
import com.securepay.modules.layout.service.SideNavService;
import com.securepay.modules.layout.entity.Colour;
import com.securepay.modules.layout.service.ColourService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/** LayoutController — real CRUD, ported from app/Modules/Layout (doc 05/08). */
@RestController
public class LayoutController extends BaseController {

    private final ResponseFactory response;
    private final ObjectMapper objectMapper;
    private final SideNavService sideNavService;
    private final ColourService colourService;

    public LayoutController(ResponseFactory response, ObjectMapper objectMapper, SideNavService sideNavService, ColourService colourService) {
        this.response = response;
        this.objectMapper = objectMapper;
        this.sideNavService = sideNavService;
        this.colourService = colourService;
    }

    // POST layout/getSideNav
    @PostMapping("layout/getSideNav")
    public ResponseEntity<Map<String, Object>> layoutGetSideNav() {
        return response.success("SideNav fetched", sideNavService.findAll());
    }

    // POST layout/createSideNav
    @PostMapping("layout/createSideNav")
    public ResponseEntity<Map<String, Object>> layoutCreateSideNav() {
        SideNav e = objectMapper.convertValue(payload(), SideNav.class);
        e.setId(null);
        return response.success("SideNav created", sideNavService.save(e));
    }

    // POST layout/updateSideNav
    @PostMapping("layout/updateSideNav")
    public ResponseEntity<Map<String, Object>> layoutUpdateSideNav() {
        Object idObj = payload().get("id");
        if (idObj == null) { return response.error("id is required", ""); }
        Optional<SideNav> ex = sideNavService.findById(Long.valueOf(idObj.toString()));
        if (ex.isEmpty()) { return response.error("SideNav not found", ""); }
        SideNav m = objectMapper.convertValue(payload(), SideNav.class);
        m.setId(ex.get().getId());
        return response.success("SideNav updated", sideNavService.save(m));
    }

    // POST layout/sideNavSchema
    @PostMapping("layout/sideNavSchema")
    public ResponseEntity<Map<String, Object>> layoutSideNavSchema() {
        return response.success("Schema", payload());
    }

    // POST layout/getColours
    @PostMapping("layout/getColours")
    public ResponseEntity<Map<String, Object>> layoutGetColours() {
        return response.success("Colour fetched", colourService.findAll());
    }

    // POST layout/createColour
    @PostMapping("layout/createColour")
    public ResponseEntity<Map<String, Object>> layoutCreateColour() {
        Colour e = objectMapper.convertValue(payload(), Colour.class);
        e.setId(null);
        return response.success("Colour created", colourService.save(e));
    }

    // POST layout/updateColour
    @PostMapping("layout/updateColour")
    public ResponseEntity<Map<String, Object>> layoutUpdateColour() {
        Object idObj = payload().get("id");
        if (idObj == null) { return response.error("id is required", ""); }
        Optional<Colour> ex = colourService.findById(Long.valueOf(idObj.toString()));
        if (ex.isEmpty()) { return response.error("Colour not found", ""); }
        Colour m = objectMapper.convertValue(payload(), Colour.class);
        m.setId(ex.get().getId());
        return response.success("Colour updated", colourService.save(m));
    }

    // POST layout/colorSchema
    @PostMapping("layout/colorSchema")
    public ResponseEntity<Map<String, Object>> layoutColorSchema() {
        return response.success("Schema", payload());
    }
}
