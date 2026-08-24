package auto.ui.api.controller;

import auto.ui.api.dto.ApiMessageDto;
import auto.ui.api.dto.ErrorCode;
import auto.ui.api.dto.ResponseListDto;
import auto.ui.api.dto.permission.PermissionDto;
import auto.ui.api.exception.NotFoundException;
import auto.ui.api.exception.UnauthorizationException;
import auto.ui.api.form.permission.CreatePermissionForm;
import auto.ui.api.form.permission.UpdatePermissionForm;
import auto.ui.api.mapper.PermissionMapper;
import auto.ui.api.model.Permission;
import auto.ui.api.model.criteria.PermissionCriteria;
import auto.ui.api.repository.PermissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/v1/permission")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Slf4j
public class PermissionController extends ABasicController {
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private PermissionMapper permissionMapper;

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('PER_C')")
    public ApiMessageDto<PermissionDto> create(@Valid @RequestBody CreatePermissionForm createPermissionForm, BindingResult bindingResult) {
        if (!isSuperAdmin()) {
            throw new UnauthorizationException("Not allowed create.");
        }
        Permission permission = permissionMapper.fromCreatePermissionFormToEntity(createPermissionForm);
        permission.setPCode(createPermissionForm.getPermissionCode());
        permissionRepository.save(permission);
        return makeSuccessResponse(permissionMapper.fromEntityToPermissionIdDto(permission), "Create permission success");
    }

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('PER_L')")
    public ApiMessageDto<ResponseListDto<List<PermissionDto>>> list(PermissionCriteria criteria, @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        if (!isSuperAdmin()) {
            throw new UnauthorizationException("Not allowed list.");
        }
        Page<Permission> permissions = permissionRepository.findAll(criteria.getSpecification(), pageable);
        return makeSuccessResponse(makeResponseListDto(permissions, permissionMapper::fromEntityToPermissionDtoList), "Get permissions list success");
    }

    @GetMapping(value = "/get/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('PER_V')")
    public ApiMessageDto<PermissionDto> get(@PathVariable("id") Long id) {
        if (!isSuperAdmin()) {
            throw new UnauthorizationException("Not allowed to get.");
        }
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Can not found permission", ErrorCode.PERMISSION_ERROR_NOT_FOUND));
        return makeSuccessResponse(permissionMapper.fromEntityToPermissionDto(permission), "Get group success");
    }

    @PutMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('PER_U')")
    public ApiMessageDto<Void> create(@Valid @RequestBody UpdatePermissionForm updatePermissionForm, BindingResult bindingResult) {
        if (!isSuperAdmin()) {
            throw new UnauthorizationException("Not allowed update.");
        }
        Permission permission = permissionRepository.findById(updatePermissionForm.getId())
                .orElseThrow(() -> new NotFoundException("Can not found permission", ErrorCode.PERMISSION_ERROR_NOT_FOUND));
        permission.setName(updatePermissionForm.getName());
        permission.setDescription(updatePermissionForm.getDescription());
        permission.setAction(updatePermissionForm.getAction());
        permission.setShowMenu(updatePermissionForm.getShowMenu());
        permission.setPCode(updatePermissionForm.getPermissionCode());
        permission.setNameGroup(updatePermissionForm.getNameGroup());
        permissionRepository.save(permission);

        return makeSuccessResponse("Update permission success");
    }
}
