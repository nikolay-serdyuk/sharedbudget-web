package sharedbudget

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import sharedbudget.entities.InputExpenseDto

@RestController
class Controller(private val service: Service) {

    @GetMapping
    fun getExpenses(
        @RequestParam(value = "date", required = false)
        date: String? = null
    ) = service.getExpenses(date)

    @PostMapping
    fun postExpenses(
        @RequestParam(value = "expenses", required = true)
        expenses: Collection<InputExpenseDto>
    ) = service.postExpenses(expenses)

    @PutMapping
    fun putExpenses(
        @RequestParam(value = "expenses", required = true)
        expenses: Collection<InputExpenseDto>
    ) = service.putExpenses(expenses)
}
