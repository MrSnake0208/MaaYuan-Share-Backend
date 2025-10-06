package plus.maa.backend.common.utils.converter

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import plus.maa.backend.controller.response.copilot.ArkLevelInfoV2
import plus.maa.backend.repository.entity.ArkLevel

@Mapper(componentModel = "spring")
interface ArkLevelConverterV2 {
    @Mapping(target = "game", expression = "java( arkLevel.getGame() == null ? \"明日方舟\" : arkLevel.getGame() )")
    fun convert(arkLevel: ArkLevel): ArkLevelInfoV2

    fun convert(arkLevels: List<ArkLevel>): List<ArkLevelInfoV2>
}

