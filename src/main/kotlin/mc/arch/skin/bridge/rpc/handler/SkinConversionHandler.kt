package mc.arch.skin.bridge.rpc.handler

import mc.arch.skin.bridge.SkinConversionQueue
import mc.arch.skin.bridge.rpc.NewSkinAvailableRequest
import mc.arch.skin.bridge.rpc.SkinConversionRequest

class SkinConversionHandler(
    private val queue: SkinConversionQueue,
    private val publisher: (NewSkinAvailableRequest) -> Unit
)
{
    fun handle(request: SkinConversionRequest)
    {
        queue.submitTask(
            playerId = request.playerId,
            base64Data = request.eaglerSkinData
        ).thenAccept { result ->
            val uploadResult = result.result
            if (uploadResult == null || uploadResult.error != null)
            {
                return@thenAccept
            }

            publisher(
                NewSkinAvailableRequest(
                    playerId = request.playerId,
                    skinValue = uploadResult.textureValue!!,
                    skinSignature = uploadResult.textureSignature!!,
                )
            )
        }.whenComplete { _, throwable ->
            throwable?.printStackTrace()
        }
    }
}
