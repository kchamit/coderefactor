package com.ibm.cleancode.unf.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class RSAProjectHelper {
	
	private static boolean isSample = true;

	private static final String[] RSA_BATCH_PROJECTS = new String[] {
			"RsaAccountMerchandising", "RsaBatchCommon", "RsaBatchWeb", "RsaColorImportBatch", "RsaCommisionsBatch",
			"RsaContractPriceImportBatch", "RsaExportContractInfoBatch", "RsaExportContractInfoCNI010Batch",
			"RsaExportPaymentBatch", "RSAExportReceivables", "RsaFlatGoodTracking", "RsaGlobalPriceImportBatch",
			"RsaHandheldScanningWearerBatch", "RsaLoadSheetGenerationBatch", "RsaNotificationBatch",
			"RsaNSFInterestChargesBatch", "RsaPriceListExportBatch", "RsaPriceListTransferBatch",
			"RsaPurchaseOrderManagementBatch", "RsaRouteExportBatch", "RsaRouteSiteExportBatch", "RsaRouteSmsBatch",
			"RSASettledInvoicesExportBatch", "RsaStaggingOnBaseContractKeyBatch", "RsaStagingCustomerExportBatch",
			"RsaStagingDsmRsrRouteCNI017ExportBatch", "RsaStagingReceivablesImportBatch",
			"RsaStagingRoadnetExportBatch", "RsaStopValueExportBatchProject", "SalesAndContractExport"
	};

	private static final String[] RSA_BILLING_PROJECTS = new String[] {
			"RsaBillingEngine", "RsaBillingEngineConfigurator", "RsaBillingEngineJPA", "RsaBillingEngineWeb",
			"RsaBillingSchedule", "RsaBillProgramManagement"
	};

	private static final String[] RSA_COMMON_PROJECTS = new String[] {
			"RsaCommon", "RsaConfigurationJPA", "RsaDasFix", "RsaHistory", "RsaReportIntegration", "RsaSearch",
			"RsaSecurityFramework", "RsaSharedDomain"
	};

	private static final String[] RSA_CORE_PROJECTS = new String[] {
			"RsaCustomer", "RsaServiceFullfillment", "RsaDynamicWeb", "RSAExportInvoice",
			"RsaExportNextDayRouteSiteInfo", "RSAExportPaymentService", "RsaLoadSheetManagement",
			"RsaMessagingService", "RsaNotificationService", "RsaPaymentGatewayDynamicWeb", "RsaPersonalization",
			"RsaRentalContract", "RsaRouteDelivery", "RsaSite", "RSAStopValueService", "RsaTransferRevenueLineItem",
			"RsaUc4Service"
	};
	
	private static final String[] CURRENT_RUN = new String[] {
		"RsaCustomer", "RsaRentalContract"
	};
	
	private static final String[] SAMPLE_PROJECTS = new String[] {
		"SampleProject"
	};

	private static final String[] RSA_WAVE1_PROJECTS = new String[] {
			"RsaCustomer", "RsaRentalContract"
	};

	public static Collection<String> getAllProjects() {
		if(isSample){
			return getSamplenProjects();
		}else{
			List<String> projects = new ArrayList<String>();
			projects.addAll(Arrays.asList(RSA_BATCH_PROJECTS));
			projects.addAll(Arrays.asList(RSA_BILLING_PROJECTS));
			projects.addAll(Arrays.asList(RSA_COMMON_PROJECTS));
			projects.addAll(Arrays.asList(RSA_CORE_PROJECTS));
			return projects;
		}
	}
	
	public static Collection<String> getSamplenProjects() {
		return Arrays.asList(SAMPLE_PROJECTS);
	}

	public static Collection<String> getBatchProjects() {
		return Arrays.asList(RSA_BATCH_PROJECTS);
	}

	public static Collection<String> getBillingProjects() {
		return Arrays.asList(RSA_BILLING_PROJECTS);
	}

	public static Collection<String> getCoreProjects() {
		return Arrays.asList(RSA_CORE_PROJECTS);
	}

	public static Collection<String> getCommonProjects() {
		return Arrays.asList(RSA_COMMON_PROJECTS);
	}
	
	public static Collection<String> getCurrentRun() {
		return Arrays.asList(CURRENT_RUN);
	}

	public static Collection<String> getAllButBillingProjects() {
		List<String> projects = new ArrayList<String>();
		projects.addAll(Arrays.asList(RSA_BATCH_PROJECTS));
		projects.addAll(Arrays.asList(RSA_COMMON_PROJECTS));
		projects.addAll(Arrays.asList(RSA_CORE_PROJECTS));
		return projects;
	}

	public static Collection<String> getWave1Projects() {
		return Arrays.asList(RSA_WAVE1_PROJECTS);
	}
}
