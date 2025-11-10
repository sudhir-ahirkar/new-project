/*
package com.toll.vendor.controller;


import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vendor/tag")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping("/{tagId}")
    public TagInfo getTag(@PathVariable String tagId) {
        return vendorService.getTagInfo(tagId);
    }
}

*/
