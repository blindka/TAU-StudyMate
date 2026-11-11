package com.tau.studymate.ui.components

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tau.studymate.data.model.Department
import com.tau.studymate.ui.theme.*

@Composable
fun DepartmentBadge(
    department: Department,
    modifier: Modifier = Modifier
) {
    val (bgColor, borderColor, textColor) = when (department) {
        Department.COMMUNICATION -> Triple(PastelSky, PastelSkyBorder, PastelSkyDark)
        Department.LABOR_STUDIES -> Triple(PastelMint, PastelMintBorder, PastelMintDark)
        Department.GENERAL -> Triple(PastelLavender, PastelLavenderBorder, PastelLavenderDark)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Text(
            text = stringResource(department.labelRes),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
