"use strict";
const common_vendor = require("../../common/vendor.js");
const api_constants_customer = require("../../api/constants/customer.js");
const utils_permissions = require("../../utils/permissions.js");
const utils_navigation = require("../../utils/navigation.js");
if (!Array) {
  const _easycom_uni_icons2 = common_vendor.resolveComponent("uni-icons");
  _easycom_uni_icons2();
}
const _easycom_uni_icons = () => "../../uni_modules/uni-icons/components/uni-icons/uni-icons.js";
if (!Math) {
  _easycom_uni_icons();
}
const _sfc_main = {
  __name: "add",
  setup(__props) {
    const steps = [
      { key: "info", label: "填写信息" },
      { key: "upload", label: "上传资质" },
      { key: "review", label: "等待审核" }
    ];
    common_vendor.onLoad(() => {
      if (!utils_permissions.hasPermission(api_constants_customer.WX_PERM.SHOP_ADD)) {
        common_vendor.index.showToast({ title: "无添加店铺权限", icon: "none" });
        utils_navigation.navigateBackDelayed(800, 1);
      }
    });
    const categories = [
      { id: 1, name: "餐饮" },
      { id: 2, name: "零售" },
      { id: 3, name: "服务" },
      { id: 4, name: "其他" }
    ];
    const categoryNames = categories.map((c) => c.name);
    const categoryIndex = common_vendor.ref(0);
    const currentStep = common_vendor.ref(1);
    const submitting = common_vendor.ref(false);
    const submitted = common_vendor.ref(false);
    const footerSafeStyle = common_vendor.computed(() => {
      var _a;
      const bottom = ((_a = common_vendor.index.getSystemInfoSync().safeAreaInsets) == null ? void 0 : _a.bottom) || 0;
      return { paddingBottom: `${Math.max(bottom, 12)}px` };
    });
    const heroDesc = common_vendor.computed(() => {
      if (submitted.value)
        return "申请已提交，请耐心等待审核结果";
      if (currentStep.value === 1)
        return "填写店铺基本信息与经营地址";
      if (currentStep.value === 2)
        return "填写资质信息并上传证照、门店实景照片";
      return "核对信息无误后提交，进入审核流程";
    });
    const fullAddress = common_vendor.computed(() => {
      return [form.province, form.city, form.district, form.address].map((s) => (s || "").trim()).filter(Boolean).join("");
    });
    const form = common_vendor.reactive({
      parentShopCode: "",
      shopName: "",
      phone: "",
      province: "",
      city: "",
      district: "",
      address: "",
      legalPerson: "",
      idCardNo: "",
      businessLicenseNo: "",
      ratio: ""
    });
    const filePaths = common_vendor.reactive({
      idCardFront: "",
      idCardBack: "",
      businessLicensePic: "",
      shopExterior: "",
      shopInterior: "",
      carouselImages: "",
      carouselImages_extra: []
    });
    const imageFields = [
      { key: "idCardFront", label: "身份证正面", required: true },
      { key: "idCardBack", label: "身份证背面", required: true },
      { key: "businessLicensePic", label: "营业执照", required: true },
      { key: "shopExterior", label: "店铺外景", required: true },
      { key: "shopInterior", label: "店铺内景", required: true },
      { key: "carouselImages", label: "轮播图", required: true, multiple: true, max: 3 }
    ];
    const carouselCount = common_vendor.computed(() => {
      let n = filePaths.carouselImages ? 1 : 0;
      n += (filePaths.carouselImages_extra || []).length;
      return n;
    });
    const uploadedImageCount = common_vendor.computed(() => {
      let n = carouselCount.value;
      for (const item of imageFields) {
        if (item.key !== "carouselImages" && filePaths[item.key])
          n += 1;
      }
      return n;
    });
    function showUploadAdd(item) {
      if (item.key === "carouselImages") {
        return carouselCount.value < item.max;
      }
      return !filePaths[item.key];
    }
    function onCategoryChange(e) {
      categoryIndex.value = Number(e.detail.value) || 0;
    }
    function scrollToTop() {
      common_vendor.index.pageScrollTo({ scrollTop: 0, duration: 0 });
    }
    function goNext() {
      if (currentStep.value === 1) {
        const err = validateStep1();
        if (err) {
          common_vendor.index.showToast({ title: err, icon: "none" });
          return;
        }
        currentStep.value = 2;
      } else if (currentStep.value === 2) {
        const err = validateStep2();
        if (err) {
          common_vendor.index.showToast({ title: err, icon: "none" });
          return;
        }
        currentStep.value = 3;
      }
      scrollToTop();
    }
    function goPrev() {
      if (currentStep.value <= 1 || submitting.value)
        return;
      currentStep.value -= 1;
      scrollToTop();
    }
    function goBackHome() {
      common_vendor.index.navigateBack();
    }
    function pickImage(key, multiple, max) {
      const remain = multiple ? max - carouselCount.value : 1;
      if (remain <= 0)
        return;
      common_vendor.index.chooseImage({
        count: multiple ? remain : 1,
        sizeType: ["compressed"],
        sourceType: ["album", "camera"],
        success: (res) => {
          const paths = res.tempFilePaths || [];
          if (!paths.length)
            return;
          if (key === "carouselImages") {
            if (!filePaths.carouselImages) {
              filePaths.carouselImages = paths[0];
              paths.slice(1).forEach((p) => filePaths.carouselImages_extra.push(p));
            } else {
              paths.forEach((p) => {
                if (carouselCount.value < max)
                  filePaths.carouselImages_extra.push(p);
              });
            }
            return;
          }
          filePaths[key] = paths[0];
        }
      });
    }
    function clearImage(key) {
      if (key === "carouselImages") {
        const extras = filePaths.carouselImages_extra || [];
        if (extras.length) {
          filePaths.carouselImages = extras.shift();
          filePaths.carouselImages_extra = extras;
        } else {
          filePaths.carouselImages = "";
          filePaths.carouselImages_extra = [];
        }
        return;
      }
      filePaths[key] = "";
    }
    function removeCarouselExtra(index) {
      filePaths.carouselImages_extra.splice(index, 1);
    }
    function previewImage(src) {
      if (!src)
        return;
      common_vendor.index.previewImage({ urls: [src], current: src });
    }
    function validateStep1() {
      if (!(form.shopName || "").trim())
        return "请输入店铺名称";
      const phone = (form.phone || "").trim();
      if (!phone)
        return "请输入联系电话";
      if (!/^1[3-9]\d{9}$/.test(phone))
        return "请输入正确的 11 位手机号";
      if (!(form.province || "").trim())
        return "请输入省份";
      if (!(form.city || "").trim())
        return "请输入城市";
      if (!(form.district || "").trim())
        return "请输入区/县";
      if (!(form.address || "").trim())
        return "请输入详细地址";
      const ratio = parseInt(String(form.ratio || "").trim(), 10);
      if (!Number.isFinite(ratio) || ratio < 1 || ratio > 1e5) {
        return "充值积分比率须为 1～100000 的正整数";
      }
      return "";
    }
    function validateStep2() {
      if (!(form.legalPerson || "").trim())
        return "请输入法人姓名";
      if (!(form.idCardNo || "").trim())
        return "请输入身份证号";
      if (!(form.businessLicenseNo || "").trim())
        return "请输入统一社会信用代码";
      for (const item of imageFields) {
        if (item.key === "carouselImages") {
          if (carouselCount.value < 1)
            return "请至少上传 1 张轮播图";
          continue;
        }
        if (item.required && !filePaths[item.key])
          return `请上传${item.label}`;
      }
      return "";
    }
    async function onSubmit() {
      var _a;
      const err = validateStep1() || validateStep2();
      if (err) {
        common_vendor.index.showToast({ title: err, icon: "none" });
        return;
      }
      const carouselImages = [filePaths.carouselImages, ...filePaths.carouselImages_extra || []].filter(
        Boolean
      );
      const ratioVal = parseInt(String(form.ratio || "").trim(), 10);
      const meta = {
        parentShopCode: (form.parentShopCode || "").trim() || void 0,
        shopName: form.shopName.trim(),
        phone: (form.phone || "").trim(),
        ratio: ratioVal,
        categoryId: (_a = categories[categoryIndex.value]) == null ? void 0 : _a.id,
        province: form.province.trim(),
        city: form.city.trim(),
        district: form.district.trim(),
        address: form.address.trim(),
        legalPerson: form.legalPerson.trim(),
        idCardNo: form.idCardNo.trim(),
        businessLicenseNo: form.businessLicenseNo.trim()
      };
      submitting.value = true;
      try {
        const { submitShopAddFull } = await "../../api/modules/shop.js";
        const result = await submitShopAddFull(meta, {
          idCardFront: filePaths.idCardFront,
          idCardBack: filePaths.idCardBack,
          businessLicensePic: filePaths.businessLicensePic,
          shopExterior: filePaths.shopExterior,
          shopInterior: filePaths.shopInterior,
          carouselImages
        });
        if (!result.ok) {
          common_vendor.index.showToast({ title: result.msg || "提交失败", icon: "none", duration: 2800 });
          return;
        }
        submitted.value = true;
        scrollToTop();
      } finally {
        submitting.value = false;
      }
    }
    return (_ctx, _cache) => {
      return common_vendor.e({
        a: common_vendor.p({
          type: submitted.value ? "checkbox-filled" : "shop-filled",
          size: 26,
          color: submitted.value ? "#34c759" : "#007aff"
        }),
        b: common_vendor.t(submitted.value ? "提交成功" : "开店申请"),
        c: common_vendor.t(heroDesc.value),
        d: common_vendor.f(steps, (step, index, i0) => {
          return common_vendor.e({
            a: index > 0
          }, index > 0 ? {
            b: submitted.value || currentStep.value > index ? 1 : ""
          } : {}, {
            c: common_vendor.t(submitted.value || currentStep.value > index + 1 ? "✓" : index + 1),
            d: common_vendor.t(step.label),
            e: !submitted.value && currentStep.value === index + 1 ? 1 : "",
            f: submitted.value || currentStep.value > index + 1 ? 1 : "",
            g: step.key
          });
        }),
        e: currentStep.value === 1 && !submitted.value
      }, currentStep.value === 1 && !submitted.value ? {
        f: form.shopName,
        g: common_vendor.o(($event) => form.shopName = $event.detail.value, "fc"),
        h: common_vendor.t(common_vendor.unref(categoryNames)[categoryIndex.value] || "请选择行业类目"),
        i: common_vendor.n(categoryIndex.value >= 0 ? "field-picker-text" : "field-picker-placeholder"),
        j: common_vendor.p({
          type: "right",
          size: 14,
          color: "#c0c4cc"
        }),
        k: common_vendor.unref(categoryNames),
        l: categoryIndex.value,
        m: common_vendor.o(onCategoryChange, "1a"),
        n: form.parentShopCode,
        o: common_vendor.o(($event) => form.parentShopCode = $event.detail.value, "88"),
        p: form.phone,
        q: common_vendor.o(($event) => form.phone = $event.detail.value, "2c"),
        r: form.ratio,
        s: common_vendor.o(($event) => form.ratio = $event.detail.value, "3f"),
        t: form.province,
        v: common_vendor.o(($event) => form.province = $event.detail.value, "72"),
        w: form.city,
        x: common_vendor.o(($event) => form.city = $event.detail.value, "bd"),
        y: form.district,
        z: common_vendor.o(($event) => form.district = $event.detail.value, "a4"),
        A: form.address,
        B: common_vendor.o(($event) => form.address = $event.detail.value, "cc")
      } : currentStep.value === 2 && !submitted.value ? {
        D: form.legalPerson,
        E: common_vendor.o(($event) => form.legalPerson = $event.detail.value, "0e"),
        F: form.idCardNo,
        G: common_vendor.o(($event) => form.idCardNo = $event.detail.value, "fc"),
        H: form.businessLicenseNo,
        I: common_vendor.o(($event) => form.businessLicenseNo = $event.detail.value, "5c"),
        J: common_vendor.f(imageFields, (item, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(item.label),
            b: item.key !== "carouselImages" && filePaths[item.key]
          }, item.key !== "carouselImages" && filePaths[item.key] ? {
            c: filePaths[item.key],
            d: common_vendor.o(($event) => clearImage(item.key), item.key),
            e: common_vendor.o(($event) => previewImage(filePaths[item.key]), item.key)
          } : item.key === "carouselImages" ? common_vendor.e({
            g: filePaths.carouselImages
          }, filePaths.carouselImages ? {
            h: filePaths.carouselImages,
            i: common_vendor.o(($event) => clearImage("carouselImages"), item.key),
            j: common_vendor.o(($event) => previewImage(filePaths.carouselImages), item.key)
          } : {}, {
            k: common_vendor.f(filePaths.carouselImages_extra || [], (extra, ei, i1) => {
              return {
                a: extra,
                b: common_vendor.o(($event) => removeCarouselExtra(ei), "c-" + ei),
                c: "c-" + ei,
                d: common_vendor.o(($event) => previewImage(extra), "c-" + ei)
              };
            })
          }) : {}, {
            f: item.key === "carouselImages",
            l: showUploadAdd(item)
          }, showUploadAdd(item) ? {
            m: "ea7900cf-2-" + i0,
            n: common_vendor.p({
              type: "plusempty",
              size: 22,
              color: "#8fa3bf"
            }),
            o: common_vendor.o(($event) => pickImage(item.key, item.multiple, item.max), item.key)
          } : {}, {
            p: item.key
          });
        })
      } : currentStep.value === 3 ? common_vendor.e({
        L: !submitted.value
      }, !submitted.value ? common_vendor.e({
        M: common_vendor.t(form.shopName.trim()),
        N: common_vendor.t(common_vendor.unref(categoryNames)[categoryIndex.value]),
        O: (form.parentShopCode || "").trim()
      }, (form.parentShopCode || "").trim() ? {
        P: common_vendor.t(form.parentShopCode.trim())
      } : {}, {
        Q: common_vendor.t(form.phone.trim()),
        R: common_vendor.t(form.ratio),
        S: common_vendor.t(fullAddress.value),
        T: common_vendor.t(form.legalPerson.trim()),
        U: common_vendor.t(uploadedImageCount.value)
      }) : {
        V: common_vendor.p({
          type: "checkbox-filled",
          size: 48,
          color: "#34c759"
        })
      }) : {}, {
        C: currentStep.value === 2 && !submitted.value,
        K: currentStep.value === 3,
        W: submitted.value
      }, submitted.value ? {
        X: common_vendor.o(goBackHome, "45")
      } : currentStep.value === 1 ? {
        Z: common_vendor.o(goNext, "f0")
      } : currentStep.value === 2 ? {
        ab: common_vendor.o(goPrev, "cc"),
        ac: common_vendor.o(goNext, "5e")
      } : {
        ad: submitting.value,
        ae: common_vendor.o(goPrev, "68"),
        af: common_vendor.t(submitting.value ? "提交中…" : "提交申请"),
        ag: submitting.value,
        ah: submitting.value,
        ai: common_vendor.o(onSubmit, "a2")
      }, {
        Y: currentStep.value === 1,
        aa: currentStep.value === 2,
        aj: common_vendor.s(footerSafeStyle.value)
      });
    };
  }
};
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__scopeId", "data-v-ea7900cf"]]);
wx.createPage(MiniProgramPage);
