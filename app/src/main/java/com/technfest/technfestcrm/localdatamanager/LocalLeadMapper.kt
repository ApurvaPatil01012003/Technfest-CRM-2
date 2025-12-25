//package com.technfest.technfestcrm.localdatamanager
//
//import com.technfest.technfestcrm.model.LeadRequest
//import com.technfest.technfestcrm.model.LeadResponseItem
//
//object LocalLeadMapper {
//
//    fun toResponse(local: LeadRequest, localId: Int): LeadResponseItem {
//        return LeadResponseItem(
//            id = localId,
//            fullName = local.fullName,
//            mobile = local.mobile,
//            status = local.status,
//            stage = local.stage,
//            priority = local.priority,
//            source = local.source,
//            sourceDetails = local.sourceDetails,
//            company = local.company,
//            location = local.location,
//            campaignId = local.campaignId,
//            campaignName = local.campaignName,
//            ownerName = local.assigned_to
//        )
//    }
//}
//


package com.technfest.technfestcrm.localdatamanager

import com.technfest.technfestcrm.model.LeadRequest
import com.technfest.technfestcrm.model.LeadResponseItem

object LocalLeadMapper {

    fun toResponse(local: LeadRequest, localId: Int): LeadResponseItem {
        return LeadResponseItem(
            id = localId,

            fullName = local.fullName ?: "",
            mobile = local.mobile ?: "",

            status = local.status ?: "",
            stage = local.stage,
            priority = local.priority ?: "",

            source = local.source,
            sourceDetails = local.sourceDetails ?: "",
            campaignId = local.campaignId ?: 0,
            campaignName = local.campaignName ?: "",

            company = local.company,
            location = local.location,
            email = local.email ?: "",

            leadRequirement = local.leadRequirement,
            nextFollowupAt = local.nextFollowupAt,


            ownerName = local.assigned_to,
            ownerUserId = 0,
            teamName = local.teamName,
            notes = local.note ?: local.note ?: "",

            createdAt = "",
            updatedAt = "",
            workspaceId = 0,
            workspaceName = "",
            workspaceCode = "",
            campaignCategoryId = 0,
            campaignCategoryName = ""
        )
    }
}
