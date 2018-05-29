package com.besafx.app.rest;

import com.besafx.app.auditing.PersonAwareUserDetails;
import com.besafx.app.config.CustomException;
import com.besafx.app.entity.*;
import com.besafx.app.search.ContractSearch;
import com.besafx.app.service.*;
import com.besafx.app.util.DateConverter;
import com.besafx.app.util.WrapperUtil;
import com.besafx.app.ws.Notification;
import com.besafx.app.ws.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.bohnman.squiggly.Squiggly;
import com.github.bohnman.squiggly.util.SquigglyUtils;
import org.jfree.util.Log;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/contract/")
public class ContractRest {

    private final static Logger LOG = LoggerFactory.getLogger(ContractRest.class);

    private final String FILTER_TABLE = "" +
            "**," +
            "customer[id,contact[id,mobile,shortName]]," +
            "seller[id,contact[id,mobile,shortName]]," +
            "-sponsor1," +
            "-sponsor2," +
            "-contractProducts," +
            "-contractPremiums," +
            "-contractPayments," +
            "-contractAttaches," +
            "person[id,contact[id,shortName]]";

    private final String FILTER_DETAILS = "" +
            "**," +
            "seller[id,contact[id,mobile,shortName]]," +
            "customer[id,contact[id,mobile,identityNumber,address,shortName]]," +
            "sponsor1[id,contact[id,mobile,shortName]]," +
            "sponsor2[id,contact[id,mobile,shortName]]," +
            "contractProducts[**,-contract,productPurchase[id,product[id,name]]]," +
            "contractPremiums[**,-contract,-contractPayments]," +
            "contractPayments[**,person[id,contact[id,shortName]],-contract,-contractPremium,-bankTransaction]," +
            "contractAttaches[**,-contract,attach[**,person[id,contact[shortName]]]]," +
            "person[id,contact[id,shortName]]";

    @Autowired
    private ContractService contractService;

    @Autowired
    private ContractProductService contractProductService;

    @Autowired
    private BankTransactionService bankTransactionService;

    @Autowired
    private ContractPaymentService contractPaymentService;

    @Autowired
    private ContractPremiumService contractPremiumService;

    @Autowired
    private ContractSearch contractSearch;

    @Autowired
    private SellerService sellerService;

    @Autowired
    private NotificationService notificationService;

    @PostMapping(value = "create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('ROLE_CONTRACT_CREATE')")
    @Transactional
    public String create(@RequestBody Contract contract) {
        Contract tempContract = contractService.findByCode(contract.getCode());
        if (tempContract != null) {
            throw new CustomException("عفواً، رقم العقد المدخل غير متاح، حاول برقم آخر");
        }
        Person caller = ((PersonAwareUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getPerson();
        contract.setPerson(caller);
        contract.setDate(new DateTime().toDate());
        contract = contractService.save(contract);
        LOG.info("ربط الأصناف المطلوبة مع العقد");
        ListIterator<ContractProduct> contractProductListIterator = contract.getContractProducts().listIterator();
        while (contractProductListIterator.hasNext()) {
            ContractProduct contractProduct = contractProductListIterator.next();
            contractProduct.setContract(contract);
            contractProductListIterator.set(contractProductService.save(contractProduct));
        }
        LOG.info("ربط الأقساط مع العقد");
        ListIterator<ContractPremium> contractPremiumListIterator = contract.getContractPremiums().listIterator();
        while (contractPremiumListIterator.hasNext()) {
            ContractPremium contractPremium = contractPremiumListIterator.next();
            contractPremium.setContract(contract);
            contractPremiumListIterator.set(contractPremiumService.save(contractPremium));
        }
        StringBuilder builder = new StringBuilder();
        builder.append("تم إنشاء العقد بنجاح بمجموع أسعار = ");
        builder.append(contract.getTotalPrice());
        builder.append("، وأصناف عدد " + contract.getContractProducts().size() + " صنف");
        builder.append("، تسدد على " + contract.getContractPremiums().size() + " قسط");
        notificationService.notifyAll(Notification
                                              .builder()
                                              .message(builder.toString())
                                              .type("success").build());
        return SquigglyUtils.stringify(Squiggly.init(new ObjectMapper(), FILTER_TABLE), contract);
    }

    @PostMapping(value = "createOld", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('ROLE_CONTRACT_CREATE')")
    @Transactional
    public String createOld(@RequestBody String wrapperUtil) {
        //Object1 represent List<ProductPurchase>
        try {
            LOG.info(wrapperUtil);

            JSONObject jsonObject_wrapper = new JSONObject(wrapperUtil);

            JSONObject jsonObject_contract = jsonObject_wrapper.getJSONObject("obj1");

            LOG.info("إنشاء العقد");
            Contract contract = new Contract();
            contract.setCode(jsonObject_contract.getLong("code"));
            contract.setDiscount(jsonObject_contract.getDouble("discount"));
            contract.setPaperFees(jsonObject_contract.getDouble("paperFees"));
            contract.setCommissionFees(jsonObject_contract.getDouble("commissionFees"));
            contract.setLawFees(jsonObject_contract.getDouble("lawFees"));
            contract.setWrittenDate(DateConverter.parseHijriDateStringWithFormat(jsonObject_contract.getString("writtenDate"), "dd/MM/yyyy"));

            LOG.info("إنشاء القسط");
            ContractPremium contractPremium = new ContractPremium();
            contractPremium.setContract(contract);
            contractPremium.setAmount(jsonObject_contract.getDouble("paid"));
            contractPremium.setDueDate(contract.getWrittenDate());

            LOG.info("إنشاء الدفعة المالية");
            ContractPayment contractPayment = new ContractPayment();
            contractPayment.setCode("");
            contractPayment.setContract(contract);
            contractPayment.setContractPremium(contractPremium);
            contractPayment.setAmount(contractPremium.getAmount());
            contractPayment.setDate(contractPremium.getDueDate());
            contractPayment.setBankTransaction();
            contractPayment.setPerson();

            LOG.info("شراء الأصناف");

            LOG.info("ربط الأصناف بالعقود");

            JSONArray jsonArray_productPurchases = jsonObject_wrapper.getJSONArray("obj2");

            LOG.info("حلقات السلع- شراء السلع ومن ثم ربطها مع العقد");
            for(int i = 0; i < jsonArray_productPurchases.length(); i++){
                JSONObject jsonObject_productPurchase = jsonArray_productPurchases.getJSONObject(i);
                LOG.info(jsonObject_productPurchase.get("unitVat").toString());
            }

        }catch (Exception ex){
            ex.printStackTrace();
        }

        return "";
    }

    @DeleteMapping(value = "delete/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ROLE_CONTRACT_DELETE')")
    @Transactional
    public void delete(@PathVariable Long id) {
        Contract contract = contractService.findOne(id);
        if (contract != null) {
            LOG.info("حذف كل سلع العقد");
            contractProductService.delete(contract.getContractProducts());
            LOG.info("حذف كل معاملات البنك لدفعات العقد");
            bankTransactionService.delete(
                    contract
                            .getContractPayments()
                            .stream()
                            .map(ContractPayment::getBankTransaction)
                            .collect(Collectors.toList())
                                         );
            LOG.info("حذف كل دفعات العقد");
            contractPaymentService.delete(contract.getContractPayments());
            LOG.info("حذف كل أقساط العقد");
            contractPremiumService.delete(contract.getContractPremiums());
            LOG.info("حذف العقد");
            contractService.delete(id);
            notificationService.notifyAll(
                    Notification
                            .builder()
                            .message("تم حذف العقد وكل ما يتعلق به من حسابات بنجاح")
                            .type("error").build());
        }
    }

    @GetMapping(value = "findOne/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String findOne(@PathVariable Long id) {
        return SquigglyUtils.stringify(Squiggly.init(new ObjectMapper(), FILTER_DETAILS),
                                       contractService.findOne(id));
    }

    @GetMapping(value = "findMyContracts", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String findMyContracts() {
        Person caller = ((PersonAwareUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getPerson();
        return SquigglyUtils.stringify(Squiggly.init(new ObjectMapper(), FILTER_DETAILS),
                                       contractService.findBySeller(caller.getCompany().getSeller()));
    }

    @GetMapping(value = "findBySeller/{sellerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String findBySeller(@PathVariable(value = "sellerId") Long sellerId) {
        return SquigglyUtils.stringify(Squiggly.init(new ObjectMapper(), FILTER_DETAILS),
                                       contractService.findBySeller(sellerService.findOne(sellerId)));
    }

    @GetMapping(value = "filter", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String filter(
            //Contract Filters
            @RequestParam(value = "codeFrom", required = false) final Integer codeFrom,
            @RequestParam(value = "codeTo", required = false) final Integer codeTo,
            @RequestParam(value = "dateFrom", required = false) final Long dateFrom,
            @RequestParam(value = "dateTo", required = false) final Long dateTo,
            //Customer Filters
            @RequestParam(value = "customerCodeFrom", required = false) final Integer customerCodeFrom,
            @RequestParam(value = "customerCodeTo", required = false) final Integer customerCodeTo,
            @RequestParam(value = "customerRegisterDateFrom", required = false) final Long customerRegisterDateFrom,
            @RequestParam(value = "customerRegisterDateTo", required = false) final Long customerRegisterDateTo,
            @RequestParam(value = "customerName", required = false) final String customerName,
            @RequestParam(value = "customerMobile", required = false) final String customerMobile,
            //Seller Filters
            @RequestParam(value = "sellerCodeFrom", required = false) final Integer sellerCodeFrom,
            @RequestParam(value = "sellerCodeTo", required = false) final Integer sellerCodeTo,
            @RequestParam(value = "sellerRegisterDateFrom", required = false) final Long sellerRegisterDateFrom,
            @RequestParam(value = "sellerRegisterDateTo", required = false) final Long sellerRegisterDateTo,
            @RequestParam(value = "sellerName", required = false) final String sellerName,
            @RequestParam(value = "sellerMobile", required = false) final String sellerMobile,
            @RequestParam(value = "filterCompareType", required = false) final String filterCompareType,
            Pageable pageable) {
        return SquigglyUtils.stringify(
                Squiggly.init(
                        new ObjectMapper(),
                        "**,".concat("content[").concat(FILTER_TABLE).concat("]")),
                contractSearch.filter(
                        codeFrom,
                        codeTo,
                        dateFrom,
                        dateTo,
                        customerCodeFrom,
                        customerCodeTo,
                        customerRegisterDateFrom,
                        customerRegisterDateTo,
                        customerName,
                        customerMobile,
                        sellerCodeFrom,
                        sellerCodeTo,
                        sellerRegisterDateFrom,
                        sellerRegisterDateTo,
                        sellerName,
                        sellerMobile,
                        filterCompareType,
                        pageable));
    }
}
