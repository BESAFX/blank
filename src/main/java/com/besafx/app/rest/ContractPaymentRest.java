package com.besafx.app.rest;

import com.besafx.app.auditing.PersonAwareUserDetails;
import com.besafx.app.entity.BankTransaction;
import com.besafx.app.entity.Contract;
import com.besafx.app.entity.ContractPayment;
import com.besafx.app.entity.Person;
import com.besafx.app.init.Initializer;
import com.besafx.app.search.ContractPaymentSearch;
import com.besafx.app.service.BankTransactionService;
import com.besafx.app.service.ContractPaymentService;
import com.besafx.app.service.ContractService;
import com.besafx.app.util.DateConverter;
import com.besafx.app.ws.Notification;
import com.besafx.app.ws.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.bohnman.squiggly.Squiggly;
import com.github.bohnman.squiggly.util.SquigglyUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;

@RestController
@RequestMapping(value = "/api/contractPayment/")
public class ContractPaymentRest {

    private final static Logger LOG = LoggerFactory.getLogger(ContractPaymentRest.class);

    private final String FILTER_TABLE = "" +
            "**," +
            "-contract," +
            "-contractPremium," +
            "-bankTransaction," +
            "person[id,contact[id,shortName]]";

    private final String FILTER_DETAILS = "" +
            "**," +
            "contract[id,code,totalPrice,seller[id,contact[id,shortName]],customer[id,contact[id,shortName,mobile]]]," +
            "-contractPremium," +
            "-bankTransaction," +
            "person[id,contact[id,shortName]]";

    @Autowired
    private ContractPaymentService contractPaymentService;

    @Autowired
    private ContractPaymentSearch contractPaymentSearch;

    @Autowired
    private ContractService contractService;

    @Autowired
    private BankTransactionService bankTransactionService;

    @Autowired
    private NotificationService notificationService;

    @PostMapping(value = "create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @PreAuthorize("hasRole('ROLE_CONTRACT_PAYMENT_CREATE')")
    @Transactional
    public String create(@RequestBody ContractPayment contractPayment) {
        ContractPayment topContractPayment = contractPaymentService.findTopByOrderByCodeDesc();
        if (topContractPayment == null) {
            contractPayment.setCode(1);
        } else {
            contractPayment.setCode(topContractPayment.getCode() + 1);
        }
        Person caller = ((PersonAwareUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getPerson();
        Contract contract = contractService.findOne(contractPayment.getContract().getId());
        contractPayment.setDate(new DateTime().toDate());
        contractPayment.setPerson(caller);

        LOG.info("عملية سداد للدفعة");
        if (contractPayment.getAmount() > 0) {
            BankTransaction bankTransaction = new BankTransaction();
            bankTransaction.setAmount(contractPayment.getAmount());
            bankTransaction.setBank(Initializer.bank);
            bankTransaction.setSeller(contract.getSeller());
            bankTransaction.setTransactionType(Initializer.transactionTypeDepositPayment);
            bankTransaction.setDate(new DateTime().toDate());
            bankTransaction.setPerson(caller);
            StringBuilder builder = new StringBuilder();
            builder.append("إيداع مبلغ نقدي بقيمة ");
            builder.append(bankTransaction.getAmount());
            builder.append("ريال سعودي، ");
            builder.append(" لـ / ");
            builder.append(bankTransaction.getSeller().getContact().getShortName());
            builder.append("، قسط مستحق بتاريخ ");
            builder.append(DateConverter.getDateInFormat(contractPayment.getContractPremium().getDueDate()));
            builder.append("، للعقد رقم / " + contract.getCode());
            bankTransaction.setNote(builder.toString());

            contractPayment.setBankTransaction(bankTransactionService.save(bankTransaction));
            contractPayment = contractPaymentService.save(contractPayment);

            notificationService.notifyAll(Notification
                                                  .builder()
                                                  .message(builder.toString())
                                                  .type("success").build());
        }
        return SquigglyUtils.stringify(Squiggly.init(new ObjectMapper(), FILTER_TABLE), contractPayment);
    }

    @DeleteMapping(value = "delete/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ROLE_CONTRACT_PAYMENT_DELETE')")
    @Transactional
    public void delete(@PathVariable Long id) {
        ContractPayment contractPayment = contractPaymentService.findOne(id);
        if (contractPayment != null) {
            bankTransactionService.delete(contractPayment.getBankTransaction());
            contractPaymentService.delete(id);
            notificationService.notifyAll(Notification
                                                  .builder()
                                                  .message("تم حذف الدفعة وكل ما يتعلق بها من حسابات بنجاح")
                                                  .type("error").build());
        }
    }

    @GetMapping(value = "findOne/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String findOne(@PathVariable Long id) {
        return SquigglyUtils.stringify(Squiggly.init(new ObjectMapper(), FILTER_TABLE),
                                       contractPaymentService.findOne(id));
    }

    @GetMapping(value = "findByContract/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String findByContract(@PathVariable Long id) {
        return SquigglyUtils.stringify(Squiggly.init(new ObjectMapper(), FILTER_TABLE),
                                       contractPaymentService.findByContractId(id));
    }

    @GetMapping(value = "findByDateBetween/{startDate}/{endDate}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String findByDateBetween(@PathVariable(value = "startDate") Long startDate, @PathVariable(value = "endDate") Long endDate) {
        return SquigglyUtils.stringify(Squiggly.init(new ObjectMapper(), FILTER_DETAILS),
                                       contractPaymentService.findByDateBetween(
                                               new DateTime(startDate).withTimeAtStartOfDay().toDate(),
                                               new DateTime(endDate).plusDays(1).withTimeAtStartOfDay().toDate()
                                      ));
    }

    @GetMapping(value = "filter", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String filter(
            //ContractPayment Filters
            @RequestParam(value = "dateFrom", required = false) final Long dateFrom,
            @RequestParam(value = "dateTo", required = false) final Long dateTo,
            //Contract Filters
            @RequestParam(value = "contractCodeFrom", required = false) final Integer contractCodeFrom,
            @RequestParam(value = "contractCodeTo", required = false) final Integer contractCodeTo,
            @RequestParam(value = "contractDateFrom", required = false) final Long contractDateFrom,
            @RequestParam(value = "contractDateTo", required = false) final Long contractDateTo,
            //Customer Filters
            @RequestParam(value = "customerName", required = false) final String customerName,
            @RequestParam(value = "customerMobile", required = false) final String customerMobile,
            //Seller Filters
            @RequestParam(value = "sellerName", required = false) final String sellerName,
            @RequestParam(value = "sellerMobile", required = false) final String sellerMobile,
            @RequestParam(value = "filterCompareType", required = false) final String filterCompareType,
            Pageable pageable) {
        return SquigglyUtils.stringify(
                Squiggly.init(
                        new ObjectMapper(),
                        "**,".concat("content[").concat(FILTER_DETAILS).concat("]")),
                contractPaymentSearch.filter(
                        dateFrom,
                        dateTo,
                        contractCodeFrom,
                        contractCodeTo,
                        contractDateFrom,
                        contractDateTo,
                        customerName,
                        customerMobile,
                        sellerName,
                        sellerMobile,
                        filterCompareType,
                        pageable));
    }
}
